package com.example.inspectorappupdate.model.card

data class CardModelData(
    val first_name: String,
    val last_name: String,
    val card_number: String,
    val valid_from: String,
    val valid_to: String,
    val indicator: String,
    val card_status: String,
    val reg_number: String,
    val support_fees_balance: Int,
    val transport_fees_balance: Int,
    val payment_status: String,
    val school_fees_balance: Int,
    val insurance_fees_balance: Int,
    val student_id: Int
)
