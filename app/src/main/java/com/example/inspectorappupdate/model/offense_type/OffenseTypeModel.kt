package com.example.inspectorappupdate.model.offense_type

data class OffenseTypeModel(
    val error: Boolean,
    val message: String,
    val data: List<OffenseTypeData>
)
