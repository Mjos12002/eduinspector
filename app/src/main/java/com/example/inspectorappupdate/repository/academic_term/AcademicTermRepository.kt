package com.example.inspectorappupdate.repository.academic_term

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.academic_term.IAcademicTerm
import com.example.inspectorappupdate.model.academic_term.AcademicTermData
import com.example.inspectorappupdate.model.academic_term.AcademicTermModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

// Repository to get the current academic term
class AcademicTermRepository() {

    // Set the required method to get the current term
    companion object SetAcademicTermAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var academicTermAPIInterface: IAcademicTerm = RetrofitInstance.getInstance().create(IAcademicTerm::class.java)
    }

    // Get the academic term
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAcademicTerm(bearer: String): AcademicTermModel {
        // Initialize the response model
        var resp: AcademicTermModel
        try {
            // Attempt a call to the api
            val res = academicTermAPIInterface.getCurrentTerm("Bearer $bearer")
            resp = if (res.code() == 200) {
                res.body()!!
            } else {

                val errorResponse = res.errorBody()?.string()
                val errorJSONObject = Gson().fromJson(errorResponse, AcademicTermModel::class.java)
                AcademicTermModel(errorJSONObject.status, errorJSONObject.error, errorJSONObject.message,
                    AcademicTermData(0, 0, "", "", ""))
            }

        } catch (e: Exception) {
            resp = AcademicTermModel(500, true, e.message.toString(),
                AcademicTermData(0, 0, "", "", ""))
        }
        return resp
    }

}