package com.example.inspectorappupdate.repository.offense

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.offense.IOffense
import com.example.inspectorappupdate.model.offense.OffenseData
import com.example.inspectorappupdate.model.offense.OffenseModel
import com.example.inspectorappupdate.model.offense.OffenseTerm
import com.example.inspectorappupdate.model.offense.OffenseType
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

class OffenseRepository() {

    companion object SetOffenseAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var offenseAPIInterface: IOffense = RetrofitInstance.getInstance().create(IOffense::class.java)
    }

    // Method used to search the student by their registration number (OLD API)
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentOffense(bearer: String, studentID: Int, termID: Int): OffenseModel {
        // Initialize the response model
        var resp: OffenseModel
        Log.i("ACADEMIC-TERM", bearer)
        try {
            // Attempt a call to the api
            val res = offenseAPIInterface.getStudentOffense("Bearer $bearer", studentID, termID)
            resp = if (res.code() == 200) {
                res.body()!!
            } else {
                val errorResponse = res.errorBody()?.string()
                val errorJSONObject = Gson().fromJson(errorResponse, OffenseModel::class.java)
                OffenseModel(res.code(), true, errorJSONObject.message, data = listOf(
                    OffenseData(0, 0, "",OffenseType(0, ""), OffenseTerm(0, ""))
                ))
            }

        } catch (e: Exception) {
            resp = OffenseModel(500, true, e.message.toString(), data = listOf(
                OffenseData(400, 0, "", OffenseType(0, ""), OffenseTerm(0, ""))
            ))
        }
        return resp
    }

}