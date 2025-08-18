package com.example.inspectorappupdate.apirequest.offense_type

import com.example.inspectorappupdate.model.offense_type.OffenseTypeModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface IOffenseType {

    @GET("/api/offense-types")
    suspend fun getOffenseType(@Header("Authorization") bearer: String): Response<OffenseTypeModel>


}