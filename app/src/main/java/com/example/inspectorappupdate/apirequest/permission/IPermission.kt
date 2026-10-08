package com.example.inspectorappupdate.apirequest.permission

import com.example.inspectorappupdate.model.permission.CancelPermissionRequest
import com.example.inspectorappupdate.model.permission.CreatePermissionRequest
import com.example.inspectorappupdate.model.permission.CreatePermissionResponse
import com.example.inspectorappupdate.model.permission.PermissionListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Student permissions to leave the school, issued by staff.
interface IPermission {

    // Permissions at the user's school still usable on a day (yyyy-MM-dd): valid, or in use (student out).
    @GET("api/visit-permissions-list")
    suspend fun list(
        @Header("Authorization") bearer: String,
        @Query("active_on") day: String,
        @Query("per_page") perPage: Int = 100
    ): Response<PermissionListResponse>

    // Cancels a permission, also while the student is out (their return is still recorded at the gate).
    @POST("api/visit-permissions/{id}/cancel")
    suspend fun cancel(@Header("Authorization") bearer: String, @Path("id") id: Int, @Body body: CancelPermissionRequest): Response<CreatePermissionResponse>

    // The school is the one the signed-in user is deployed at.
    @POST("api/visit-permissions-create")
    suspend fun create(@Header("Authorization") bearer: String, @Body body: CreatePermissionRequest): Response<CreatePermissionResponse>
}
