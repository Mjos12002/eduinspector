package com.example.inspectorappupdate.apirequest.card

import com.example.inspectorappupdate.model.card.ChipCardRequest
import com.example.inspectorappupdate.model.card.ChipEnvelope
import com.example.inspectorappupdate.model.card.ChipPassword
import com.example.inspectorappupdate.model.card.ChipSetupRequest
import com.example.inspectorappupdate.model.card.ChipTapRequest
import com.example.inspectorappupdate.model.card.ChipTapState
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// The card's chip: its password to set it up, the set-up outcome, and what a tap read on screens that only look up.
interface ICardChip {

    @POST("api/card-chip/password")
    suspend fun password(@Header("Authorization") bearer: String, @Body body: ChipCardRequest): Response<ChipEnvelope<ChipPassword>>

    @POST("api/card-chip/setup")
    suspend fun setup(@Header("Authorization") bearer: String, @Body body: ChipSetupRequest): Response<ChipEnvelope<Any>>

    @POST("api/card-chip/tap")
    suspend fun tap(@Header("Authorization") bearer: String, @Body body: ChipTapRequest): Response<ChipEnvelope<ChipTapState>>
}
