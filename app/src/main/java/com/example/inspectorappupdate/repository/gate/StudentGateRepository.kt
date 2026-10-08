package com.example.inspectorappupdate.repository.gate

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.gate.IStudentGate
import com.example.inspectorappupdate.model.gate.StudentGateRequest
import com.example.inspectorappupdate.model.gate.StudentGateResult
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

/** A tap's outcome: the server's decision, or why the tap couldn't be sent. */
sealed class GateTapResult {
    data class Decided(val result: StudentGateResult) : GateTapResult()
    data class Failed(val message: String) : GateTapResult()
}

// Network errors come back as Failed, never thrown.
class StudentGateRepository {

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private val api: IStudentGate = RetrofitInstance.getInstance().create(IStudentGate::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun tap(token: String, cardNumber: String, direction: String, deviceId: String?): GateTapResult {
        return try {
            val response = api.tap("Bearer $token", StudentGateRequest(cardNumber, direction, deviceId))
            val decided = response.body()?.data
            if (response.isSuccessful && decided != null) {
                GateTapResult.Decided(decided)
            } else {
                GateTapResult.Failed(errorMessage(response.errorBody()?.string(), response.code()))
            }
        } catch (e: IOException) {
            GateTapResult.Failed("No connection to the server. Check the internet and tap again.")
        } catch (e: Exception) {
            GateTapResult.Failed(e.message ?: "Something went wrong")
        }
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
