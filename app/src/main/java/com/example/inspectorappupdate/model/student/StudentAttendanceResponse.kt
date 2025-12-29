package com.example.inspectorappupdate.model.student

// StudentAttendanceResponse is used to model the response of the student attendance creation
data class StudentAttendanceResponse (
    val status: Int,
    val error: Boolean,
    val message: String
)