package com.example.inspectorappupdate.repository.permission

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.permission.IPermission
import com.example.inspectorappupdate.model.permission.CancelPermissionRequest
import com.example.inspectorappupdate.model.permission.CreatePermissionRequest
import com.example.inspectorappupdate.model.permission.Permission
import com.example.inspectorappupdate.repository.visitor.VisitorResult
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import java.io.IOException

// API calls for student permissions. Network errors are returned as Failure, never thrown.
class PermissionRepository {

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private val api: IPermission = RetrofitInstance.getInstance().create(IPermission::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun list(token: String, day: String): VisitorResult<List<Permission>> =
        call { api.list("Bearer $token", day) }.map { it.data?.permissions.orEmpty() }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun create(token: String, request: CreatePermissionRequest): VisitorResult<String?> =
        call { api.create("Bearer $token", request) }.map { it.message }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun cancel(token: String, id: Int, reason: String?): VisitorResult<String?> =
        call { api.cancel("Bearer $token", id, CancelPermissionRequest(reason)) }.map { it.message }

    private fun <T, R> VisitorResult<T>.map(transform: (T) -> R): VisitorResult<R> = when (this) {
        is VisitorResult.Success -> VisitorResult.Success(transform(data), message)
        is VisitorResult.Failure -> this
    }

    private suspend fun <T> call(request: suspend () -> Response<T>): VisitorResult<T> = try {
        val response = request()
        val body = response.body()
        if (response.isSuccessful && body != null) VisitorResult.Success(body, null)
        else VisitorResult.Failure(errorMessage(response.errorBody()?.string(), response.code()))
    } catch (e: IOException) {
        VisitorResult.Failure("No connection to the server. Check the internet and try again.")
    } catch (e: Exception) {
        VisitorResult.Failure(e.message ?: "Something went wrong")
    }

    private fun errorMessage(errorBody: String?, code: Int): String {
        val message = try {
            Gson().fromJson(errorBody, JsonObject::class.java)?.get("message")?.takeIf { !it.isJsonNull }?.asString
        } catch (e: Exception) {
            null
        }
        return message ?: when (code) {
            401 -> "Your session has expired. Sign in again."
            403 -> "You are not allowed to do this."
            else -> "Request failed ($code)"
        }
    }
}
