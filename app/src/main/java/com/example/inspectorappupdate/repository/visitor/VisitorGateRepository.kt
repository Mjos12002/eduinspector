package com.example.inspectorappupdate.repository.visitor

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.visitor.IVisitorGate
import com.example.inspectorappupdate.model.visitor.AttachCardRequest
import com.example.inspectorappupdate.model.visitor.GateRecordRequest
import com.example.inspectorappupdate.model.visitor.GateScanRequest
import com.example.inspectorappupdate.model.visitor.GateState
import com.example.inspectorappupdate.model.visitor.GateStateResponse
import com.example.inspectorappupdate.model.visitor.RegisterStudent
import com.example.inspectorappupdate.model.visitor.RegisterVisitorRequest
import com.example.inspectorappupdate.model.visitor.VisitorCardItem
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import java.io.IOException

/** Outcome of a call: the payload on success, otherwise a message the inspector can read. */
sealed class VisitorResult<out T> {
    data class Success<T>(val data: T, val message: String?) : VisitorResult<T>()
    data class Failure(val message: String) : VisitorResult<Nothing>()
}

// API calls for students' visitor cards. Network errors are returned as Failure, never thrown.
class VisitorGateRepository {

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private val api: IVisitorGate = RetrofitInstance.getInstance().create(IVisitorGate::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scan(token: String, cardNumber: String, deviceId: String?): VisitorResult<GateState> =
        state { api.scan("Bearer $token", GateScanRequest(cardNumber, deviceId)) }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun checkIn(token: String, cardNumber: String, deviceId: String?, visitorIds: List<Int>): VisitorResult<GateState> =
        state { api.record("Bearer $token", GateRecordRequest(cardNumber, deviceId, visitorIds)) }

    // Everyone inside on this card leaves: the card goes out with them.
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun checkOutAll(token: String, cardNumber: String, deviceId: String?): VisitorResult<GateState> =
        state { api.record("Bearer $token", GateRecordRequest(cardNumber, deviceId, emptyList(), checkOutAll = true)) }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun lookupCards(token: String, search: String): VisitorResult<List<VisitorCardItem>> =
        call { api.lookupCards("Bearer $token", search) }.let { result ->
            when (result) {
                is VisitorResult.Success -> VisitorResult.Success(result.data.data ?: emptyList(), result.message)
                is VisitorResult.Failure -> result
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun cardInfo(token: String, uid: String): VisitorResult<GateState> =
        state { api.cardInfo("Bearer $token", uid) }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun attachCard(token: String, cardId: Int, cardNumber: String, chipType: String?): VisitorResult<VisitorCardItem> =
        call { api.attachCard("Bearer $token", cardId, AttachCardRequest(cardNumber, chipType)) }.let { result ->
            when (result) {
                is VisitorResult.Success -> result.data.data?.let { VisitorResult.Success(it, result.message) }
                    ?: VisitorResult.Failure(result.message ?: "Unexpected response")
                is VisitorResult.Failure -> result
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun studentByCard(token: String, uid: String): VisitorResult<RegisterStudent> =
        call { api.studentByCard("Bearer $token", uid) }.let { result ->
            when (result) {
                is VisitorResult.Success -> result.data.data?.let { VisitorResult.Success(it, result.message) }
                    ?: VisitorResult.Failure(result.message ?: "Unexpected response")
                is VisitorResult.Failure -> result
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registerVisitor(token: String, request: RegisterVisitorRequest): VisitorResult<String?> =
        call { api.registerVisitor("Bearer $token", request) }.let { result ->
            when (result) {
                is VisitorResult.Success -> VisitorResult.Success(result.message, result.message)
                is VisitorResult.Failure -> result
            }
        }

    private suspend fun state(request: suspend () -> Response<GateStateResponse>): VisitorResult<GateState> =
        call(request).let { result ->
            when (result) {
                is VisitorResult.Success -> result.data.data?.let { VisitorResult.Success(it, result.message) }
                    ?: VisitorResult.Failure(result.message ?: "Unexpected response")
                is VisitorResult.Failure -> result
            }
        }

    private suspend fun <T> call(request: suspend () -> Response<T>): VisitorResult<T> {
        return try {
            val response = request()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                VisitorResult.Success(body, messageOf(body))
            } else {
                VisitorResult.Failure(errorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: IOException) {
            VisitorResult.Failure("No connection to the server. Check the internet and tap again.")
        } catch (e: Exception) {
            VisitorResult.Failure(e.message ?: "Something went wrong")
        }
    }

    private fun messageOf(body: Any): String? = try {
        Gson().toJsonTree(body).asJsonObject.get("message")?.takeIf { !it.isJsonNull }?.asString
    } catch (e: Exception) {
        null
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
