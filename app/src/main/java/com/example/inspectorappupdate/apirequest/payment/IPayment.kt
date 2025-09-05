package com.example.inspectorappupdate.apirequest.payment

import com.example.inspectorappupdate.model.payment.PaymentTransactionModel
import com.example.inspectorappupdate.model.payment.PaymentTypeModel
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// API request to get the payment type
interface IPayment {

    @GET("api/payment-type")
    suspend fun getPaymentType(@Header("Authorization") bearer: String): Response<PaymentTypeModel>

    @FormUrlEncoded
    @POST("api/create-topup")
    suspend fun makePayment(@Header(value = "Authorization") bearer: String, @Field("amount") amount: Int, @Field("topup_type_id") topup_type_id: Int, @Field("payment_type_id") payment_type_id: Int, @Field("phone") phone: String, @Field("student_id") student_id: Int, @Field("parent_id") parent_id: Int, @Field("card_number") card_number: String): Response<PaymentTransactionModel>
}
