package com.example.inspectorappupdate.dto.student

//StudentAttendance is used to model the data transfer object for creating the attendance of the student
data class StudentAttendanceDTO(
    val student_identifier: String,
    val class_name: String,
    val attendance_status: String,
    val attendance_date: String,
    val institute_id: Int,
    val notes: String
)
