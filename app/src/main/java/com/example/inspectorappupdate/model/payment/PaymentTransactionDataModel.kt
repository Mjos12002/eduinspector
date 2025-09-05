package com.example.inspectorappupdate.model.payment

data class PaymentTransactionDataModel(
    val id: Int,
    val amount: Int,
    val payment_status: String
)
