package com.example.inspectorappupdate.model.academic_term

data class AcademicTermModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: AcademicTermData
) {}