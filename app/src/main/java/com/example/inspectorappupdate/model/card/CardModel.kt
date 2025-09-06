package com.example.inspectorappupdate.model.card

// Model of the card
data class CardModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: CardModelData
)
