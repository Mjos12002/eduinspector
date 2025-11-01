package com.example.inspectorappupdate.model.card

data class DisablePinModel(
    val status: Int,
    val error: Boolean,
    val message: String,
)