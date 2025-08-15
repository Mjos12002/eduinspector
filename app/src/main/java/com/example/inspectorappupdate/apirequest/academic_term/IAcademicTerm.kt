package com.example.inspectorappupdate.apirequest.academic_term

import com.example.inspectorappupdate.model.academic_term.AcademicTermModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

// Interface to get the current term
interface IAcademicTerm {

    // Call the end point to get current term
    @GET("/api/get-current-term")
    suspend fun getCurrentTerm(@Header("Authorization") bearer: String): Response<AcademicTermModel>

}