package com.example.inspectorappupdate.viewmodel.gate

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inspectorappupdate.repository.gate.GateTapResult
import com.example.inspectorappupdate.repository.gate.StudentGateRepository
import com.example.inspectorappupdate.utils.CardChip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The student gate screen: the chosen direction and the result of the last tap, recorded on the tap itself.
// The result clears a few seconds after the card is taken away.
class StudentGateViewModel : ViewModel() {

    enum class Direction(val api: String) { IN("in"), OUT("out") }

    private val repository = StudentGateRepository()

    private val _direction = MutableLiveData(Direction.IN)
    val direction: LiveData<Direction> = _direction

    private val _result = MutableLiveData<GateTapResult?>(null)
    val result: LiveData<GateTapResult?> = _result

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var clearJob: Job? = null

    fun setDirection(direction: Direction) {
        if (_direction.value == direction) return
        _direction.value = direction
        _result.value = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onCardTapped(token: String, uid: String, deviceId: String?, chip: CardChip.Reading? = null) {
        if (_loading.value == true) return
        clearJob?.cancel()
        _loading.value = true
        viewModelScope.launch {
            _result.value = repository.tap(token, uid, (_direction.value ?: Direction.IN).api, deviceId, chip)
            _loading.value = false
        }
    }

    fun onCardRemoved() {
        clearJob?.cancel()
        clearJob = viewModelScope.launch {
            delay(CLEAR_AFTER_MS)
            _result.value = null
        }
    }

    companion object {
        private const val CLEAR_AFTER_MS = 4000L
    }
}
