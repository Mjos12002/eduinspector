package com.example.inspectorappupdate.model.student

data class StudentUserResponseModel(
    val id: Int,
    val current_institute_id: Int,
    val status: String,
    val parent_user_id: Int
)