package com.example.inspectorappupdate.model.academic_year

data class AcademicYearModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: AcademicYearData
)
