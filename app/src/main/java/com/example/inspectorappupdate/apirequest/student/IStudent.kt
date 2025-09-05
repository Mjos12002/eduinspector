package com.example.inspectorappupdate.apirequest.student

import com.example.inspectorappupdate.model.student.StudentModel
import com.example.inspectorappupdate.model.student.StudentSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface IStudent {

    @GET("api/show/{reg_number}/student")
    suspend fun searchStudentByRegistrationNumber(@Header("Authorization") bearer: String,  @Path("reg_number") reg_number: String): Response<StudentModel>


    @GET("api/show/{reg_number}/student")
    suspend fun getStudentByRegistrationNumber(@Header("Authorization") bearer: String,  @Path("reg_number") reg_number: String): Response<StudentSearchResponse>

}