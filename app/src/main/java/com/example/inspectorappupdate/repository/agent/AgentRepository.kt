package com.example.inspectorappupdate.repository.agent

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.apirequest.agent.IAgent
import com.example.inspectorappupdate.model.agent.AgentEnvelope
import com.example.inspectorappupdate.model.agent.AgentPage
import com.example.inspectorappupdate.model.agent.AgentStatsData
import com.example.inspectorappupdate.model.agent.AgentStudent
import com.example.inspectorappupdate.model.agent.BlockCardRequest
import com.example.inspectorappupdate.model.agent.ChipCheck
import com.example.inspectorappupdate.model.agent.ChipCheckRequest
import com.example.inspectorappupdate.model.agent.IssueCardRequest
import com.example.inspectorappupdate.model.agent.IssueResult
import com.example.inspectorappupdate.model.agent.StockCard
import com.example.inspectorappupdate.model.agent.UpdateStudentRequest
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import java.io.IOException

/** A call's outcome: the payload, or a message for the agent and the server's reason code. */
sealed class AgentResult<out T> {
    data class Success<T>(val data: T, val message: String?) : AgentResult<T>()
    data class Failure(val message: String, val code: String? = null, val offline: Boolean = false) : AgentResult<Nothing>()
}

// Network errors come back as Failure, never thrown.
@RequiresApi(Build.VERSION_CODES.O)
class AgentRepository {

    companion object {
        private val api: IAgent by lazy { RetrofitInstance.getInstance().create(IAgent::class.java) }
    }

    suspend fun search(token: String, term: String): AgentResult<List<AgentStudent>> = call({ api.search(bearer(token), term) }) { it.data }

    suspend fun student(token: String, id: Int): AgentResult<AgentStudent> = call({ api.student(bearer(token), id) }) { it.data }

    suspend fun updateStudent(token: String, id: Int, gender: String?, classId: Int?): AgentResult<AgentStudent> =
        call({ api.updateStudent(bearer(token), id, UpdateStudentRequest(gender, classId)) }) { it.data }

    suspend fun block(token: String, cardId: Int, reason: String): AgentResult<AgentStudent> =
        call({ api.block(bearer(token), cardId, BlockCardRequest(reason)) }) { it.data }

    suspend fun check(token: String, studentId: Int, uid: String, signature: String? = null): AgentResult<ChipCheck> =
        call({ api.check(bearer(token), ChipCheckRequest(studentId, uid, signature)) }) { it.data }

    suspend fun issue(token: String, studentId: Int, uid: String, pin: String, confirmation: String,
                      signature: String? = null, counter: Int? = null): AgentResult<IssueResult> =
        call({ api.issue(bearer(token), IssueCardRequest(studentId, uid, pin, confirmation, signature, counter)) }) { it.data }

    suspend fun stats(token: String): AgentResult<AgentStatsData> = call({ api.stats(bearer(token)) }) { it.data }

    suspend fun stock(token: String): AgentResult<List<StockCard>> = call({ api.stock(bearer(token)) }) { it.data }

    private fun bearer(token: String) = "Bearer $token"

    private suspend fun <B, T> call(request: suspend () -> Response<B>, payload: (B) -> T?): AgentResult<T> = try {
        val response = request()
        val body = response.body()
        val data = body?.let(payload)
        if (response.isSuccessful && data != null) {
            AgentResult.Success(data, message(body))
        } else {
            failure(response.errorBody()?.string(), response.code())
        }
    } catch (e: IOException) {
        AgentResult.Failure("No connection to the server. Check the internet and try again.", offline = true)
    } catch (e: Exception) {
        AgentResult.Failure(e.message ?: "Something went wrong")
    }

    private fun message(body: Any?): String? = when (body) {
        is AgentEnvelope<*> -> body.message
        is AgentPage<*> -> body.message
        else -> null
    }

    private fun failure(errorBody: String?, code: Int): AgentResult.Failure {
        val json = try { Gson().fromJson(errorBody, JsonObject::class.java) } catch (e: Exception) { null }
        val message = json?.get("message")?.takeIf { !it.isJsonNull }?.asString
        val reason = json?.get("code")?.takeIf { !it.isJsonNull }?.asString
        return AgentResult.Failure(message ?: when (code) {
            401 -> "Your session has expired. Sign in again."
            403 -> "You are not allowed to do this."
            else -> "Request failed ($code)"
        }, reason)
    }
}
