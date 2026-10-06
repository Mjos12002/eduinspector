package com.example.inspectorappupdate.viewmodel.visitor

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inspectorappupdate.model.visitor.GateState
import com.example.inspectorappupdate.model.visitor.RegisterStudent
import com.example.inspectorappupdate.model.visitor.RegisterVisitorRequest
import com.example.inspectorappupdate.model.visitor.RegisterVisitorStudent
import com.example.inspectorappupdate.model.visitor.VisitorCardItem
import com.example.inspectorappupdate.repository.visitor.VisitorGateRepository
import com.example.inspectorappupdate.repository.visitor.VisitorResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Visitor screen state. Requests run in viewModelScope so a tap in flight is not cancelled (and its
// outcome lost) when the inspector leaves the screen.
class VisitorGateViewModel : ViewModel() {

    companion object {
        const val MIN_SEARCH_LENGTH = 2
        private const val SEARCH_DEBOUNCE_MS = 300L
        // Once the card is taken off the phone, the gate and attach screens clear after this long without a tick or button press.
        private const val GATE_IDLE_CLEAR_MS = 3_000L
    }

    enum class Mode { GATE, ATTACH, REGISTER }

    // Gate tab: recording arrivals or departures.
    enum class Direction { IN, OUT }

    private val repository = VisitorGateRepository()

    private val _mode = MutableLiveData(Mode.GATE)
    val mode: LiveData<Mode> = _mode

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    // The card tapped at the gate: its student and visitors, refreshed after recording.
    private val _gateState = MutableLiveData<VisitorResult<GateState>?>()
    val gateState: LiveData<VisitorResult<GateState>?> = _gateState

    private val _direction = MutableLiveData(Direction.IN)
    val direction: LiveData<Direction> = _direction

    fun setDirection(direction: Direction) {
        if (_direction.value == direction) return
        _direction.value = direction
        _selection.value = emptySet()
        _recordError.value = null
        _gateState.value = null
    }

    // Visitors ticked on the gate screen (external_visitors.id).
    private val _selection = MutableLiveData<Set<Int>>(emptySet())
    val selection: LiveData<Set<Int>> = _selection

    // Why recording failed; the visitor list stays on screen so the staff can correct and retry.
    private val _recordError = MutableLiveData<String?>()
    val recordError: LiveData<String?> = _recordError

    // Who was just checked in, shown briefly once the screen is cleared for the next card.
    private val _notice = MutableLiveData<String?>()
    val notice: LiveData<String?> = _notice

    fun noticeShown() {
        _notice.value = null
    }

    private var gateUid: String? = null
    private var gateDeviceId: String? = null

    private val _lookup = MutableLiveData<VisitorResult<List<VisitorCardItem>>?>()
    val lookup: LiveData<VisitorResult<List<VisitorCardItem>>?> = _lookup

    private val _selectedCard = MutableLiveData<VisitorCardItem?>()
    val selectedCard: LiveData<VisitorCardItem?> = _selectedCard

    private val _attachResult = MutableLiveData<VisitorResult<VisitorCardItem>?>()
    val attachResult: LiveData<VisitorResult<VisitorCardItem>?> = _attachResult

    // Register visitor: the student whose card was tapped, and the result of the last save.
    private val _registerStudent = MutableLiveData<VisitorResult<RegisterStudent>?>()
    val registerStudent: LiveData<VisitorResult<RegisterStudent>?> = _registerStudent

    private val _registerResult = MutableLiveData<VisitorResult<String?>?>()
    val registerResult: LiveData<VisitorResult<String?>?> = _registerResult

    private var registerUid: String? = null

    fun clearRegister() {
        registerUid = null
        _registerStudent.value = null
        _registerResult.value = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerVisitor(token: String, fullName: String, phone: String?, relationship: String) {
        val student = (_registerStudent.value as? VisitorResult.Success)?.data ?: return
        if (_busy.value == true) return
        run {
            val result = repository.registerVisitor(
                token,
                RegisterVisitorRequest(fullName, phone, listOf(RegisterVisitorStudent(student.studentId, relationship)))
            )
            _registerResult.value = result
            // Refresh the student so the new visitor shows in their list.
            if (result is VisitorResult.Success) registerUid?.let { _registerStudent.value = repository.studentByCard(token, it) }
        }
    }

    fun registerResultShown() {
        _registerResult.value = null
    }

    // Whose card the last tapped chip is (attach mode).
    private val _cardInfo = MutableLiveData<VisitorResult<GateState>?>()
    val cardInfo: LiveData<VisitorResult<GateState>?> = _cardInfo

    // Modes the signed-in role may use; the screen hides the mode switch when there is only one.
    private val _allowedModes = MutableLiveData(listOf(Mode.GATE, Mode.ATTACH))
    val allowedModes: LiveData<List<Mode>> = _allowedModes

    fun setAllowedModes(modes: List<Mode>) {
        _allowedModes.value = modes
        if (_mode.value !in modes) setMode(modes.first())
    }

    fun setMode(mode: Mode) {
        if (mode !in _allowedModes.value.orEmpty()) return
        _mode.value = mode
        _selectedCard.value = null
        _attachResult.value = null
        clearRegister()
    }

    fun selectCard(card: VisitorCardItem?) {
        _selectedCard.value = card
        _attachResult.value = null
        _cardInfo.value = null
    }

    /** Called for every NFC tap while this screen is open. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun onCardTapped(token: String, uid: String, chipType: String?, deviceId: String?) {
        if (_busy.value == true) return

        when (_mode.value) {
            Mode.REGISTER -> {
                registerUid = uid
                _registerResult.value = null
                run { _registerStudent.value = repository.studentByCard(token, uid) }
            }
            Mode.ATTACH -> {
                val card = _selectedCard.value
                idleJob?.cancel()
                if (card == null) {
                    // No card chosen: show whose card this is.
                    run {
                        _cardInfo.value = repository.cardInfo(token, uid)
                        restartIdleTimer()
                    }
                } else {
                    run {
                        val result = repository.attachCard(token, card.id, uid, chipType)
                        _attachResult.value = result
                        // A chip that is already in use: show whose it is.
                        _cardInfo.value = if (result is VisitorResult.Failure) repository.cardInfo(token, uid) else null
                        restartIdleTimer()
                    }
                }
            }
            else -> {
                gateUid = uid
                gateDeviceId = deviceId
                _selection.value = emptySet()
                _recordError.value = null
                // Check out needs no choice: everyone inside on the card leaves with it.
                idleJob?.cancel()
                run {
                    _gateState.value =
                        if (_direction.value == Direction.OUT) repository.checkOutAll(token, uid, deviceId)
                        else repository.scan(token, uid, deviceId)
                    restartIdleTimer()
                }
            }
        }
    }

    fun toggleVisitor(visitorId: Int) {
        val current = _selection.value.orEmpty()
        _selection.value = if (visitorId in current) current - visitorId else current + visitorId
        restartIdleTimer()
    }

    /** Records the ticked visitors as coming in (Check in tab). */
    @RequiresApi(Build.VERSION_CODES.O)
    fun record(token: String) {
        val uid = gateUid ?: return
        val state = (_gateState.value as? VisitorResult.Success)?.data ?: return
        val ticked = _selection.value.orEmpty()
        if (ticked.isEmpty() || _busy.value == true) return

        val checkIn = state.visitors.orEmpty().filter { it.id in ticked && it.inside != true }.map { it.id }
        if (checkIn.isEmpty()) return

        idleJob?.cancel()
        run {
            when (val result = repository.checkIn(token, uid, gateDeviceId, checkIn)) {
                // Checked in: clear the screen for the next card.
                is VisitorResult.Success -> {
                    gateUid = null
                    _selection.postValue(emptySet())
                    _recordError.postValue(null)
                    _gateState.postValue(null)
                    val names = result.data.checkedIn.orEmpty()
                    _notice.postValue(if (names.isEmpty()) result.message else "Checked in: ${names.joinToString(", ")}")
                }
                is VisitorResult.Failure -> {
                    _recordError.value = result.message
                    restartIdleTimer()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private var searchJob: Job? = null

    // Searches after a pause in typing; a newer search cancels the one in flight so an older response
    // can't replace newer results.
    fun search(token: String, term: String, immediate: Boolean = false) {
        searchJob?.cancel()
        val query = term.trim()
        if (query.length < MIN_SEARCH_LENGTH) {
            _lookup.value = null
            _busy.value = false
            return
        }
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            _busy.value = true
            try {
                _lookup.value = repository.lookupCards(token, query)
            } finally {
                if (searchJob == coroutineContext[Job]) _busy.value = false
            }
        }
    }

    private var idleJob: Job? = null

    // While the tapped card is held against the phone the screen stays up.
    private var cardPresent = false

    fun onCardPresent() {
        cardPresent = true
        idleJob?.cancel()
    }

    fun onCardRemoved() {
        if (!cardPresent) return
        cardPresent = false
        // The register form is filled in after the card is taken away, so it stays.
        if (_mode.value == Mode.REGISTER) return
        if (_gateState.value != null || _cardInfo.value != null || _attachResult.value != null) restartIdleTimer()
    }

    private fun restartIdleTimer() {
        idleJob?.cancel()
        if (cardPresent) return
        idleJob = viewModelScope.launch {
            delay(GATE_IDLE_CLEAR_MS)
            if (_mode.value == Mode.ATTACH) clearAttach() else clearGate()
        }
    }

    // After the card is taken away: an attached card returns to the search for the next one, a failed
    // attach keeps the chosen card waiting for another tap.
    private fun clearAttach() {
        when (_attachResult.value) {
            is VisitorResult.Success -> selectCard(null)
            is VisitorResult.Failure -> _attachResult.value = null
            null -> Unit
        }
        _cardInfo.value = null
    }

    fun clearGate() {
        idleJob?.cancel()
        gateUid = null
        _selection.value = emptySet()
        _recordError.value = null
        _gateState.value = null
    }

    private fun run(block: suspend () -> Unit) {
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.postValue(false)
            }
        }
    }
}
