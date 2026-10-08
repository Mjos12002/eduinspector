package com.example.inspectorappupdate.viewmodel.permission

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inspectorappupdate.model.permission.CreatePermissionRequest
import com.example.inspectorappupdate.model.permission.Permission
import com.example.inspectorappupdate.model.permission.PermissionStudent
import com.example.inspectorappupdate.repository.permission.PermissionRepository
import com.example.inspectorappupdate.repository.visitor.VisitorResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Permissions screen: permissions usable on the chosen day, and issuing one for the tapped student card.
@RequiresApi(Build.VERSION_CODES.O)
class PermissionViewModel : ViewModel() {

    private val repository = PermissionRepository()

    private val _day = MutableLiveData(Calendar.getInstance())
    val day: LiveData<Calendar> = _day

    private val _permissions = MutableLiveData<VisitorResult<List<Permission>>?>()
    val permissions: LiveData<VisitorResult<List<Permission>>?> = _permissions

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _student = MutableLiveData<PermissionStudent?>()
    val student: LiveData<PermissionStudent?> = _student

    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving

    // Result of the last submit: whether it succeeded, and a message.
    private val _saveResult = MutableLiveData<Pair<Boolean, String>?>()
    val saveResult: LiveData<Pair<Boolean, String>?> = _saveResult

    fun setDay(token: String, day: Calendar) {
        _day.value = day
        load(token)
    }

    fun load(token: String) {
        val day = DAY.format(_day.value!!.time)
        _loading.value = true
        viewModelScope.launch {
            _permissions.value = repository.list(token, day)
            _loading.value = false
        }
    }

    fun setStudent(student: PermissionStudent?) {
        _student.value = student
        _saveResult.value = null
    }

    fun create(token: String, request: CreatePermissionRequest) {
        if (_saving.value == true) return
        _saving.value = true
        viewModelScope.launch {
            when (val result = repository.create(token, request)) {
                is VisitorResult.Success -> {
                    _saveResult.value = true to "Permission issued"
                    _student.value = null
                    load(token)
                }
                is VisitorResult.Failure -> _saveResult.value = false to result.message
            }
            _saving.value = false
        }
    }

    // Message of the last cancel, shown once.
    private val _cancelResult = MutableLiveData<String?>()
    val cancelResult: LiveData<String?> = _cancelResult

    fun cancel(token: String, id: Int) {
        if (_loading.value == true) return
        _loading.value = true
        viewModelScope.launch {
            _cancelResult.value = when (val result = repository.cancel(token, id, null)) {
                is VisitorResult.Success -> result.data ?: "Permission cancelled"
                is VisitorResult.Failure -> result.message
            }
            load(token)
        }
    }

    fun cancelResultShown() {
        _cancelResult.value = null
    }

    fun saveResultShown() {
        _saveResult.value = null
    }

    companion object {
        private val DAY = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
