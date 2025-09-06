package com.example.inspectorappupdate.apirequest.card

import com.example.inspectorappupdate.model.card.CardModel
import com.example.inspectorappupdate.model.card.ChangePinModel
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// API request to get the card
interface ICard {

    // Get card detaills
    @GET("api/show/{card_number}/card/number")
    suspend fun getCardByNumber(@Header("Authorization") bearer: String,  @Path("card_number") card_number: String): Response<CardModel>

    // Change pin
    @FormUrlEncoded
    @POST("api/change-pin")
    suspend fun changePIN(@Header("Authorization") bearer: String, @Field("old_pin") old_pin: String, @Field("new_pin") new_pin: String, @Field("card_number") card_number: String): Response<ChangePinModel>

}