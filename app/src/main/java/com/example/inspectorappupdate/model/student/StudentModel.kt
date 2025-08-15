package com.example.inspectorappupdate.model.student

// A model to describe the data returned the api request
data class StudentModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: StudentData
)




