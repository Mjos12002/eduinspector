package com.example.inspectorappupdate.model.offense

data class OffenseData(
    val id: Int,
    val marks_deducted: Int,
    val offense_date: String,
    val offense_type: OffenseType,
    val term: OffenseTerm
)
