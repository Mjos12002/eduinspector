package com.example.inspectorappupdate.model.offense_type

data class OffenseTypeData(
    val id: Int,
    val type_name: String,
    val mark_deduction: Double? = null,
    val severity_level: Int? = null,
    val punishment: String? = null,
    val offense_category: OffenseCategoryRef? = null
)

data class OffenseCategoryRef(
    val id: Int,
    val name: String?
)
