package com.example.inspectorappupdate.viewmodel.student

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.model.student.StudentModel

// View model of the user response
class StudentViewModel: ViewModel() {

    // Initialize the variable of the student mutable live data
    private var _studentMutableLiveData = MutableLiveData<StudentModel>()
    // Initialize the variable of the student live data
    var studentLiveData: LiveData<StudentModel> = _studentMutableLiveData

    // Function to get the student
    fun searchStudentByRegNumber(regNumber: String, token: String) {

    }
}