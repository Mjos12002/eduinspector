package com.example.inspectorappupdate.apirequest.offense

import com.example.inspectorappupdate.model.offense.OffenseModel
import com.example.inspectorappupdate.model.offense.StudentOffenseCreationModel
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Interface to manage the request for offense
interface IOffense {

    @GET("api/student-offense/student/term/{student_id}/{term_id}")
    suspend fun getStudentOffense(@Header("Authorization") bearer: String, @Path("student_id") student_id: Int, @Path("term_id") term_id: Int): Response<OffenseModel>

    @FormUrlEncoded
    @POST("api/student-offense")
    suspend fun createStudentOffense(@Header("Authorization") bearer: String, @Field("student_id") student_id: Int, @Field("offense_type_id") offense_type_id: Int, @Field("academic_year_id") academic_year_id: Int, @Field("term_id") term_id: Int, @Field("offense_date") offense_date: String, @Field("institute_id") institute_id: Int): Response<StudentOffenseCreationModel>

}