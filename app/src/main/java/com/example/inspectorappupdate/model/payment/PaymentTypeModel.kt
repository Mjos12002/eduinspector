package com.example.inspectorappupdate.model.payment

// Modelling the payment type
data class PaymentTypeModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: List<PaymentTypeModelData>
)