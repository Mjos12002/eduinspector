package com.example.inspectorappupdate.apirequest.agent

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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Card agents: find students at their schools, correct class and gender, block old cards, issue new ones.
interface IAgent {

    @GET("api/agent/students")
    suspend fun search(@Header("Authorization") bearer: String, @Query("search") term: String): Response<AgentPage<AgentStudent>>

    @GET("api/agent/students/{id}")
    suspend fun student(@Header("Authorization") bearer: String, @Path("id") id: Int): Response<AgentEnvelope<AgentStudent>>

    @PATCH("api/agent/students/{id}")
    suspend fun updateStudent(@Header("Authorization") bearer: String, @Path("id") id: Int, @Body body: UpdateStudentRequest): Response<AgentEnvelope<AgentStudent>>

    @POST("api/agent/cards/{id}/block")
    suspend fun block(@Header("Authorization") bearer: String, @Path("id") cardId: Int, @Body body: BlockCardRequest): Response<AgentEnvelope<AgentStudent>>

    @POST("api/agent/cards/check")
    suspend fun check(@Header("Authorization") bearer: String, @Body body: ChipCheckRequest): Response<AgentEnvelope<ChipCheck>>

    @POST("api/agent/cards/issue")
    suspend fun issue(@Header("Authorization") bearer: String, @Body body: IssueCardRequest): Response<AgentEnvelope<IssueResult>>

    @GET("api/agent/stats")
    suspend fun stats(@Header("Authorization") bearer: String): Response<AgentEnvelope<AgentStatsData>>

    @GET("api/agent/stock")
    suspend fun stock(@Header("Authorization") bearer: String): Response<AgentEnvelope<List<StockCard>>>
}
