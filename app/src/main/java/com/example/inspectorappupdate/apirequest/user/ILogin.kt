package com.example.inspectorappupdate.apirequest.user

import com.example.inspectorappupdate.apiresponse.user.LoginResponse
import com.example.inspectorappupdate.model.auth.DeployedUserModel
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ILogin {

    // login is used to login the user
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(@Field("reg_number") reg_number: String, @Field("password") password: String): Response<LoginResponse>

    //getDeployedUser is used to get deployed users
    @GET("api/show/{user_id}/user-deployed")
    suspend fun getDeployedUser(@Header("Authorization") bearer: String, @Path("user_id") user_id: Int): Response<DeployedUserModel>
}