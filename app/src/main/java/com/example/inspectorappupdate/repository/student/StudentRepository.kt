package com.example.inspectorappupdate.repository.student

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.student.IStudent
import com.example.inspectorappupdate.model.student.StudentModelData
import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import com.example.inspectorappupdate.model.student.StudentUserResponseModel
import com.example.inspectorappupdate.model.student.UserResponseModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson


class StudentRepository() {


    // Object used to create the user interface to trigger the API call
    companion object SetStudentAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var studentAPIInterface: IStudent = RetrofitInstance.getInstance().create(IStudent::class.java)
    }

    // Method used to search the student by their registration number (OLD API)
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

}