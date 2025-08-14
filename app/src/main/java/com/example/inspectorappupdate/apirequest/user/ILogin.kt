package com.example.inspectorappupdate.apirequest.user

import com.example.inspectorappupdate.apiresponse.user.LoginResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ILogin {
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(@Field("reg_number") reg_number: String, @Field("password") password: String): Response<LoginResponse>
}