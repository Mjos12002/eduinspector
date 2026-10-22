package com.example.inspectorappupdate.viewmodel.agent

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inspectorappupdate.model.agent.AgentStudent
import com.example.inspectorappupdate.model.agent.IssueResult
import com.example.inspectorappupdate.repository.agent.AgentRepository
import com.example.inspectorappupdate.repository.agent.AgentResult
import com.example.inspectorappupdate.repository.card.CardChipRepository
import com.example.inspectorappupdate.utils.CardChip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Issuing a card, one step at a time: find the student, check class and gender, block the old card,
// tap the new card, the student enters the PIN twice, confirm. The PIN lives here only until it is sent.
@RequiresApi(Build.VERSION_CODES.O)
class CardIssueViewModel : ViewModel() {

    enum class Step { SEARCH, STUDENT, TAP, PIN, CONFIRM, DONE }

    private val repository = AgentRepository()

    private val _step = MutableLiveData(Step.SEARCH)
    val step: LiveData<Step> = _step

    private val _results = MutableLiveData<AgentResult<List<AgentStudent>>?>(null)
    val results: LiveData<AgentResult<List<AgentStudent>>?> = _results

    private val _student = MutableLiveData<AgentStudent?>(null)
    val student: LiveData<AgentStudent?> = _student

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    /** One-off messages (saved, blocked, failures) shown as a toast. */
    private val _notice = MutableLiveData<String?>(null)
    val notice: LiveData<String?> = _notice

    /** Why the tapped card, the PIN or the activation was refused, shown on that step. */
    private val _stepError = MutableLiveData<String?>(null)
    val stepError: LiveData<String?> = _stepError

    /** How many PIN digits are typed, and whether this is the second entry. */
    private val _pinDigits = MutableLiveData(0)
    val pinDigits: LiveData<Int> = _pinDigits
    private val _pinRepeat = MutableLiveData(false)
    val pinRepeat: LiveData<Boolean> = _pinRepeat

    private val _issued = MutableLiveData<IssueResult?>(null)
    val issued: LiveData<IssueResult?> = _issued

    var tappedUid: String? = null
        private set

    private var searchJob: Job? = null
    private val pin = StringBuilder()
    private var firstPin: String? = null
    private var tappedChip: CardChip.Reading? = null
    // Set-up outcomes by chip, waiting for the chip to be issued.
    private val chipSetups = mutableMapOf<String, CardChip.SetupResult>()
    private val chipRepository = CardChipRepository()

    fun search(token: String, term: String) {
        val trimmed = term.trim()
        searchJob?.cancel()
        if (trimmed.length < 2) {
            _results.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            _busy.value = true
            _results.value = repository.search(token, trimmed)
            _busy.value = false
        }
    }

    fun openStudent(token: String, id: Int) {
        reset()
        _step.value = Step.STUDENT
        load(token) { repository.student(token, id) }
    }

    fun refreshStudent(token: String) {
        val id = _student.value?.id ?: return
        load(token) { repository.student(token, id) }
    }

    fun saveDetails(token: String, gender: String?, classId: Int?) {
        val current = _student.value ?: return
        val genderChange = gender?.takeIf { it != current.gender?.lowercase() }
        val classChange = classId?.takeIf { it != current.classId }
        if (genderChange == null && classChange == null) return
        load(token, "Student updated") { repository.updateStudent(token, current.id, genderChange, classChange) }
    }

    fun blockCard(token: String, reason: String) {
        val card = _student.value?.activeCard ?: return
        load(token, "Card blocked. You can now register the new card.") { repository.block(token, card.id, reason) }
    }

    /** Opens the tap step; the old card must be blocked first. */
    fun startRegistration() {
        val current = _student.value ?: return
        if (current.hasActiveCard) {
            _notice.value = "Block the current card first."
            return
        }
        tappedUid = null
        tappedChip = null
        _stepError.value = null
        _step.value = Step.TAP
    }

    fun onCardTapped(token: String, uid: String, chip: CardChip.Reading? = null) {
        val current = _student.value ?: return
        if (_step.value != Step.TAP || _busy.value == true) return
        _busy.value = true
        _stepError.value = null
        viewModelScope.launch {
            when (val result = repository.check(token, current.id, uid, chip?.signature)) {
                is AgentResult.Success -> if (result.data.ok) {
                    tappedUid = uid
                    tappedChip = chip?.takeIf { it.uid == uid }
                    clearPin()
                    _step.value = Step.PIN
                } else {
                    _stepError.value = result.data.message
                }
                is AgentResult.Failure -> _stepError.value = result.message
            }
            _busy.value = false
        }
    }

    fun typeDigit(digit: Char) {
        if (pin.length >= PIN_LENGTH) return
        pin.append(digit)
        _pinDigits.value = pin.length
        _stepError.value = null
        if (pin.length < PIN_LENGTH) return

        val entered = pin.toString()
        val first = firstPin
        if (first == null) {
            firstPin = entered
            pin.clear()
            _pinDigits.value = 0
            _pinRepeat.value = true
        } else if (first == entered) {
            _step.value = Step.CONFIRM
        } else {
            clearPin()
            _stepError.value = "The two PINs are different. Enter a new PIN."
        }
    }

    fun deleteDigit() {
        if (pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        _pinDigits.value = pin.length
    }

    fun activate(token: String) {
        val current = _student.value ?: return
        val uid = tappedUid ?: return
        val chosen = firstPin ?: return
        if (_busy.value == true) return
        _busy.value = true
        _stepError.value = null
        viewModelScope.launch {
            val chip = tappedChip
            when (val result = repository.issue(token, current.id, uid, chosen, pin.toString(), chip?.signature, chip?.counter)) {
                is AgentResult.Success -> {
                    clearPin()
                    _issued.value = result.data
                    reportChipSetup(token, uid)
                    result.data.student?.let { _student.value = it }
                    _step.value = Step.DONE
                }
                is AgentResult.Failure -> {
                    _stepError.value = result.message
                    // A refused chip goes back to tapping; a lost connection can be retried as is.
                    if (result.code == "CARD_TAKEN" || result.code == "CARD_RETIRED" || result.code == "VISITOR_CARD") {
                        tappedUid = null
                        clearPin()
                        _step.value = Step.TAP
                    } else if (result.code == "HAS_ACTIVE_CARD") {
                        clearPin()
                        _step.value = Step.STUDENT
                        refreshStudent(token)
                    }
                }
            }
            _busy.value = false
        }
    }

    /** One step back; true when there was a step to go back to. */
    fun back(): Boolean {
        when (_step.value) {
            Step.STUDENT, Step.DONE -> { reset(); _step.value = Step.SEARCH }
            Step.TAP -> _step.value = Step.STUDENT
            Step.PIN, Step.CONFIRM -> { clearPin(); _step.value = Step.TAP }
            else -> return false
        }
        _stepError.value = null
        return true
    }

    /** After a card is issued: back to an empty search for the next student. */
    fun nextStudent() {
        reset()
        _results.value = null
        _step.value = Step.SEARCH
    }

    fun noticeShown() {
        _notice.value = null
    }

    private fun load(token: String, success: String? = null, request: suspend () -> AgentResult<AgentStudent>) {
        _busy.value = true
        viewModelScope.launch {
            when (val result = request()) {
                is AgentResult.Success -> {
                    // Detail replies carry the class list; edits keep the one already loaded.
                    val classes = result.data.classes ?: _student.value?.classes
                    _student.value = result.data.copy(classes = classes)
                    if (success != null) _notice.value = success
                }
                is AgentResult.Failure -> {
                    _notice.value = result.message
                    if (_student.value == null) _step.value = Step.SEARCH
                }
            }
            _busy.value = false
        }
    }

    /** The outcome of setting our password on the tapped chip; reported once the chip is the student's card. */
    fun onChipSetUp(token: String, uid: String, result: CardChip.SetupResult) {
        chipSetups[uid] = result
        if (_issued.value?.card?.cardNumber.equals(uid, ignoreCase = true)) reportChipSetup(token, uid)
    }

    private fun reportChipSetup(token: String, uid: String) {
        val result = chipSetups.remove(uid) ?: return
        val signature = tappedChip?.takeIf { it.uid == uid }?.signature
        viewModelScope.launch { chipRepository.reportSetup(token, uid, result, signature) }
    }

    private fun clearPin() {
        pin.clear()
        firstPin = null
        _pinDigits.value = 0
        _pinRepeat.value = false
    }

    private fun reset() {
        clearPin()
        tappedUid = null
        tappedChip = null
        _student.value = null
        _issued.value = null
        _stepError.value = null
    }

    companion object {
        const val PIN_LENGTH = 4
        private const val SEARCH_DELAY_MS = 350L
    }
}
