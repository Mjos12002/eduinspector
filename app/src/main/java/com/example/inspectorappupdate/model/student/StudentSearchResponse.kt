package com.example.inspectorappupdate.model.student

data class StudentSearchResponse(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: UserResponseModel
)