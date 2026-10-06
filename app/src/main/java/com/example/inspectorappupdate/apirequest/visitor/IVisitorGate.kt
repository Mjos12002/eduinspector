package com.example.inspectorappupdate.apirequest.visitor

import com.example.inspectorappupdate.model.visitor.AttachCardRequest
import com.example.inspectorappupdate.model.visitor.GateRecordRequest
import com.example.inspectorappupdate.model.visitor.GateScanRequest
import com.example.inspectorappupdate.model.visitor.GateStateResponse
import com.example.inspectorappupdate.model.visitor.RegisterStudentResponse
import com.example.inspectorappupdate.model.visitor.RegisterVisitorRequest
import com.example.inspectorappupdate.model.visitor.RegisterVisitorResponse
import com.example.inspectorappupdate.model.visitor.VisitorCardLookupResponse
import com.example.inspectorappupdate.model.visitor.VisitorCardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Students' visitor cards: gate entry/exit and attaching physical cards.
interface IVisitorGate {

    // A card tapped at the gate: the student and their visitors, or why it is refused (logged).
    @POST("api/visitor-gate/scan")
    suspend fun scan(@Header("Authorization") bearer: String, @Body body: GateScanRequest): Response<GateStateResponse>

    // Records which visitors came in, or checks out everyone inside on the card.
    @POST("api/visitor-gate/tap")
    suspend fun record(@Header("Authorization") bearer: String, @Body body: GateRecordRequest): Response<GateStateResponse>

    // Cards by printed code (e.g. VC-22-000123), student name, or visitor name or phone.
    @GET("api/visitor-register/cards/lookup")
    suspend fun lookupCards(@Header("Authorization") bearer: String, @Query("search") search: String): Response<VisitorCardLookupResponse>

    // Whose card a chip is, without recording anything.
    @GET("api/gate/card/{uid}")
    suspend fun cardInfo(@Header("Authorization") bearer: String, @Path("uid") uid: String): Response<GateStateResponse>

    // The student a tapped student card belongs to, with their visitors and visitor card.
    @GET("api/visitor-register/student-card/{uid}")
    suspend fun studentByCard(@Header("Authorization") bearer: String, @Path("uid") uid: String): Response<RegisterStudentResponse>

    // Registers a visitor for a student; the student's visitor card waits for a physical card.
    @POST("api/visitor-register/visitors")
    suspend fun registerVisitor(@Header("Authorization") bearer: String, @Body body: RegisterVisitorRequest): Response<RegisterVisitorResponse>

    @POST("api/visitor-register/cards/{id}/attach")
    suspend fun attachCard(@Header("Authorization") bearer: String, @Path("id") cardId: Int, @Body body: AttachCardRequest): Response<VisitorCardResponse>
}
