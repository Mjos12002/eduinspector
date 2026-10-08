package com.example.inspectorappupdate.apirequest.gate

import com.example.inspectorappupdate.model.gate.StudentGateRequest
import com.example.inspectorappupdate.model.gate.StudentGateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Student cards at the gate: the server decides permission exit/return or daily check-in/out and records it.
interface IStudentGate {

    @POST("api/gate/student-tap")
    suspend fun tap(@Header("Authorization") bearer: String, @Body body: StudentGateRequest): Response<StudentGateResponse>
}
