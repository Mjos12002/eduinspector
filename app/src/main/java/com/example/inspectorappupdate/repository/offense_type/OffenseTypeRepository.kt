package com.example.inspectorappupdate.repository.offense_type

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.offense_type.IOffenseType
import com.example.inspectorappupdate.model.offense_type.OffenseTypeData
import com.example.inspectorappupdate.model.offense_type.OffenseTypeModel
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson

class OffenseTypeRepository() {

    companion object SetOffenseTypeAPIInterface {
        @RequiresApi(Build.VERSION_CODES.O)
        private var offenseTypeAPIInterface: IOffenseType = RetrofitInstance.getInstance().create(IOffenseType::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getOffenseType(bearer: String): OffenseTypeModel {
        // Initialize the response model
        var resp: OffenseTypeModel
        try {
            // Attempt a call to the api
            val res = offenseTypeAPIInterface.getOffenseType("Bearer $bearer")
            resp = if (res.code() == 200) {
                res.body()!!
            } else {
                val errorResponse = res.errorBody()?.string()
                val errorJSONObject = Gson().fromJson(errorResponse, OffenseTypeModel::class.java)
                OffenseTypeModel(true, errorJSONObject.message, data = listOf(
                    OffenseTypeData(0, ""))
                )
            }

        } catch (e: Exception) {
            resp = OffenseTypeModel(true, e.message.toString(), data = listOf(
                OffenseTypeData(0, ""))
            )
        }
        return resp
    }

}