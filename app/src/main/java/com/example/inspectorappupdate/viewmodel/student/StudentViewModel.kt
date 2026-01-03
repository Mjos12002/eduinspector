package com.example.inspectorappupdate.viewmodel.student

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inspectorappupdate.dto.student.StudentAttendanceDTO
import com.example.inspectorappupdate.model.student.SchoolAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import com.example.inspectorappupdate.model.student.StudentUserResponseModel
import com.example.inspectorappupdate.model.student.UserResponseModel
import com.example.inspectorappupdate.model.studentpermission.StudentPermissionResponse
import com.example.inspectorappupdate.repository.student.StudentRepository

// View model of the user response
class StudentViewModel: ViewModel() {

    // Initialize the variable of the student mutable live data
    private var _studentMutableLiveData = MutableLiveData<StudentModel>()
    private var _studentDetailsMutableLiveData = MutableLiveData<StudentSearchResponse>()

    // Initialize the variable of the class attendance in mutable livedata
    private var _classAttendanceInMutableLiveData = MutableLiveData<SchoolAttendanceResponse>()

    // Initialize the variable of the class attendance in mutable livedata
    private var _classAttendanceOutMutableLiveData = MutableLiveData<SchoolAttendanceResponse>()

    // Initialize the variable of the student permission mutable livedata
    private var _studentPermissionMutableLiveData = MutableLiveData<StudentPermissionResponse>()

    // Initialize the variable of the student attendance response
    private var _classRoomAttendanceMutableLiveData = MutableLiveData<StudentAttendanceResponse>()


    // Initialize the variable of the student live data
    var studentPermissionLiveData: LiveData<StudentPermissionResponse> = _studentPermissionMutableLiveData
    var studentLiveData: LiveData<StudentModel> = _studentMutableLiveData
    // Initialize the variable of student detail live data
    var studentDetailsLiveData: LiveData<StudentSearchResponse> = _studentDetailsMutableLiveData

    // Initialize the variable of the class attendance in live data
    var classAttendanceInLiveData: LiveData<SchoolAttendanceResponse> = _classAttendanceInMutableLiveData

    // Initialize the variable of the class attendance out live data
    var classAttendanceOutLiveData: LiveData<SchoolAttendanceResponse> = _classAttendanceOutMutableLiveData
    var classRoomAttendanceLiveData: LiveData<StudentAttendanceResponse>  = _classRoomAttendanceMutableLiveData


    //createStudentClassRoomAttendance is used to manage the class student room attendance
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createStudentClassRoomAttendance(bearer: String, classRoomAttendanceDTO: StudentAttendanceDTO){
        val response = StudentRepository().createStudentAttendance(bearer, classRoomAttendanceDTO)
        _classRoomAttendanceMutableLiveData.postValue(response)
    }

    // Function to get the student
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun searchStudentByRegNumber(bearer: String, regNumber: String) {
        // Get the response from the api and update the mutable live data object
        val response =  StudentRepository().getStudentByRegNo(bearer, regNumber)
        _studentMutableLiveData.postValue(response)

    }

    // getStudentPermission is used to get the student permission
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentPermission(bearer: String, studentID: String) {
        try{
            val permissionResponse = StudentRepository().getStudentPermission(bearer, studentID)
            _studentPermissionMutableLiveData.postValue(permissionResponse)
        }catch (e: Exception) {
            Log.i("TAG-INFORMATION", e.message!!)
        }
    }

    // getStudentDetails is used to get the student details
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentDetails(bearer: String, regNumber: String) {
        // Get the response from the api and update the mutable live data object
        val response = StudentRepository().getStudentDetails(bearer, regNumber)
        _studentDetailsMutableLiveData.postValue(response)

    }

    // clearStudentViewModel is used to clear the view model
    suspend fun clearStudentViewModel(){
        _studentDetailsMutableLiveData.postValue(StudentSearchResponse(0, true, "",
            UserResponseModel(0, "", "", "", "", "", "", "", "", "", "", StudentUserResponseModel(0, 0, "", 0))))
    }

    //createSchoolAttendanceIn is used to post the information about school attendance in
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSchoolAttendanceIn(bearer: String, cardNumber: String, deviceID: Int) {
        val response = StudentRepository().addSchoolAttendanceIn(bearer, cardNumber, deviceID)
        _classAttendanceInMutableLiveData.postValue(response)
    }

    //createSchoolAttendanceOut is used to post the information about school attendance in
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSchoolAttendanceOut(bearer: String, cardNumber: String, deviceID: Int) {
        val response = StudentRepository().addSchoolAttendanceOut(bearer, cardNumber, deviceID)
        _classAttendanceOutMutableLiveData.postValue(response)
    }
}