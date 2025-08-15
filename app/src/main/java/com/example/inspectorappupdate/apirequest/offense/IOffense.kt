package com.example.inspectorappupdate.apirequest.offense

import com.example.inspectorappupdate.model.offense.OffenseModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// Interface to manage the request for offense
interface IOffense {

    @GET("api/student-offense/student/term/{student_id}/{term_id}")
    suspend fun getStudentOffense(@Header("Authorization") bearer: String, @Path("student_id") student_id: Int, @Path("term_id") term_id: Int): Response<OffenseModel>

}