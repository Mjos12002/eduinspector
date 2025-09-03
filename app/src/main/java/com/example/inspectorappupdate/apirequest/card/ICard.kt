package com.example.inspectorappupdate.apirequest.card

import com.example.inspectorappupdate.model.card.CardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// API request to get the card
interface ICard {

    @GET("api/show/{card_number}/card/number")
    suspend fun getCardByNumber(@Header("Authorization") bearer: String,  @Path("card_number") card_number: String): Response<CardResponse>

}