package com.example.inspectorappupdate.apirequest.wallet

import com.example.inspectorappupdate.model.wallet.WalletModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

// Interface for accessing the wallet API
interface IWallet {

    @GET("api/topup-type")
    suspend fun getWallet(@Header("Authorization") bearer: String): Response<WalletModel>

}