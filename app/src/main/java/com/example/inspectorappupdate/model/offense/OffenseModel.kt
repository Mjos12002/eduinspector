package com.example.inspectorappupdate.model.offense

data class OffenseModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    var data: List<OffenseData>
)