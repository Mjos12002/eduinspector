package com.example.inspectorappupdate.viewmodel.student

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import com.example.inspectorappupdate.repository.student.StudentRepository

// View model of the user response
class StudentViewModel: ViewModel() {

    // Initialize the variable of the student mutable live data
    private var _studentMutableLiveData = MutableLiveData<StudentModel>()
    private var _studentDetailsMutableLiveData = MutableLiveData<StudentSearchResponse>()
    // Initialize the variable of the student live data
    var studentLiveData: LiveData<StudentModel> = _studentMutableLiveData
    var studentDetailsLiveData: LiveData<StudentSearchResponse> = _studentDetailsMutableLiveData
    // Function to get the student
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun searchStudentByRegNumber(bearer: String, regNumber: String) {
        // Get the response from the api and update the mutable live data object
        val response =  StudentRepository().getStudentByRegNo(bearer, regNumber)
        _studentMutableLiveData.postValue(response)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentDetails(bearer: String, regNumber: String) {
        // Get the response from the api and update the mutable live data object
        val response = StudentRepository().getStudentDetails(bearer, regNumber)
        Log.i("INSPECTOR-LOG", "$response")
        _studentDetailsMutableLiveData.postValue(response)

    }
}