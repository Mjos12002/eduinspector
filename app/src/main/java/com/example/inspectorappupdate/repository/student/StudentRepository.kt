package com.example.inspectorappupdate.repository.student

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.student.IStudent
import com.example.inspectorappupdate.dto.student.StudentAttendanceDTO
import com.example.inspectorappupdate.model.student.SchoolAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentAttendanceResponse
import com.example.inspectorappupdate.model.student.StudentModelData
import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import com.example.inspectorappupdate.model.student.StudentUserResponseModel
import com.example.inspectorappupdate.model.student.UserResponseModel
import com.example.inspectorappupdate.model.studentpermission.StudentPermissionResponse
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson


class StudentRepository() {


    // Object used to create the user interface to trigger the API call
    companion object SetStudentAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var studentAPIInterface: IStudent = RetrofitInstance.getInstance().create(IStudent::class.java)
    }

    // getStudentByRegNo is used to search the student by their registration number (OLD API)
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentByRegNo(bearer: String, regNumber: String): StudentModel {
        // Initialize the response model
        try {
            // Attempt a call to the api
            val res = studentAPIInterface.searchStudentByRegistrationNumber("Bearer $bearer", regNumber)
             if (res.code() == 200) {
                return res.body()!!
            } else {
                val errorResponse = res.errorBody()?.string()
                val errorJSONObject = Gson().fromJson(errorResponse, StudentModel::class.java)
               return StudentModel(res.code(), true, errorJSONObject.message, data = StudentModelData(0, "", "", ""))
            }

        } catch (e: Exception) {

            return StudentModel(500, true, e.message.toString(), data = StudentModelData(0, "", "", ""))

        }

    }

    // getStudentDetails
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentDetails(bearer: String, regNumber: String): StudentSearchResponse {

        try {
            val res = studentAPIInterface.getStudentByRegistrationNumber("Bearer $bearer", regNumber)
            if (res.code() == 200) {
                return res.body()!!
            }else {
                // Error response
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, StudentSearchResponse::class.java)

                return StudentSearchResponse(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message,
                    UserResponseModel(0, "", "", "", "", "", "", "", "", "", "",
                        StudentUserResponseModel(0, 0, "", 0)))

            }
        }catch (e: Exception) {

            return StudentSearchResponse(500, true, e.message!!,
                UserResponseModel(0, "", "", "", "", "", "", "", "", "", "",
                    StudentUserResponseModel(0, 0, "", 0)))

        }

    }

    //addSchoolAttendanceOut is used to record attendance in
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addSchoolAttendanceIn(bearer: String, cardNumber: String, deviceID: Int): SchoolAttendanceResponse {

        try{
            // Get the response from the end point and process the response accordingly
            val res = studentAPIInterface.addSchoolAttendanceIn("Bearer $bearer", cardNumber, deviceID)
            if (res.code() == 200 || res.code() == 201) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, SchoolAttendanceResponse::class.java)
                return SchoolAttendanceResponse(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message)
            }

        }catch (e: Exception) {
            return SchoolAttendanceResponse(true, e.message!!, "Uknown error, try again")
        }
    }

    //addSchoolAttendanceOut is used to record attendance out
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addSchoolAttendanceOut(bearer: String, cardNumber: String, deviceID: Int): SchoolAttendanceResponse {

        try{
            // Get the response from the end point and process the response accordingly
            val res = studentAPIInterface.addSchoolAttendanceOut("Bearer $bearer", cardNumber, deviceID)
            if (res.code() == 200 || res.code() == 201) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, SchoolAttendanceResponse::class.java)
                return SchoolAttendanceResponse(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message)
            }

        }catch (e: Exception) {
            return SchoolAttendanceResponse(true, e.message!!, "Unknown error, contact admin")
        }
    }

    // getStudentPermission is used to get the student permissions
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentPermission(bearer: String, studentID: String): StudentPermissionResponse {
        try {
            // Get the response from the API, if a response is successful return the body otherwise return a structured error response
            val res = studentAPIInterface.getStudentPermission("Bearer $bearer", studentID)
            if (res.code() == 200 || res.code() == 201) {
                return res.body()!!
            }else {
                val errorBody = res.errorBody()?.string()
                val errorBodyObj = Gson().fromJson(errorBody, StudentPermissionResponse::class.java)
                return StudentPermissionResponse(errorBodyObj.status, errorBodyObj.error, errorBodyObj.message, null)
            }
        }catch (e: Exception) {
            return StudentPermissionResponse(500, "Error, contact admin", e.message!!, null)
        }
    }

    suspend fun createStudentAttendance(bearer: String, studentAttendance: StudentAttendanceDTO): StudentAttendanceResponse {
        try {



        }catch (e: Exception) {

        }
    }

}