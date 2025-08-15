package com.example.inspectorappupdate.model.academic_term

data class AcademicTermData(
    val id: Int,
    val academic_year_id: Int,
    val term_name: String,
    val start_date: String,
    val end_date: String
)
