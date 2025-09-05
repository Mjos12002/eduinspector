package com.example.inspectorappupdate.model.payment

data class PaymentTransactionModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: PaymentTransactionDataModel
)