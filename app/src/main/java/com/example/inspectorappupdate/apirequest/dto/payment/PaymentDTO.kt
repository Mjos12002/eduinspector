package com.example.inspectorappupdate.apirequest.dto.payment

data class PaymentDTO(
    val amount: Int,
    val topup_type_id: Int,
    val payment_type_id: Int,
    val phone: String,
    val student_id: Int,
    val parent_id: Int,
    val card_number: String
)
