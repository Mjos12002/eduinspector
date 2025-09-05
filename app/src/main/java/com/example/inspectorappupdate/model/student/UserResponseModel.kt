package com.example.inspectorappupdate.model.student

data class UserResponseModel(
    val id: Int,
    val reg_number: String,
    val first_name: String,
    val last_name: String,
    val photo: String,
    val date_of_birth: String,
    val gender: String,
    val address: String,
    val contact_number: String,
    val status: String,
    val email: String,
    val student: StudentUserResponseModel
)