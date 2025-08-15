package com.example.inspectorappupdate.repository.student

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.student.IStudent
import com.example.inspectorappupdate.model.student.StudentData
import com.example.inspectorappupdate.model.student.StudentModel
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
        var resp: StudentModel
        try {
            // Attempt a call to the api
            val res = studentAPIInterface.searchStudentByRegistrationNumber("Bearer $bearer", regNumber)
            resp = if (res.code() == 200) {
                res.body()!!
            } else {

                val errorResponse = res.errorBody()?.string()
                val errorJSONObject = Gson().fromJson(errorResponse, StudentModel::class.java)
                StudentModel(res.code(), true, errorJSONObject.message, data = StudentData(0, "", "", ""))
            }

        } catch (e: Exception) {
            resp = StudentModel(500, true, e.message.toString(), data = StudentData(0, "", "", ""))
        }
        return resp
    }

}