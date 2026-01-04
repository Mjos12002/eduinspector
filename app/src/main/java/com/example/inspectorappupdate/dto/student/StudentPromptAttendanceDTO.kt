package com.example.inspectorappupdate.dto.student

// StudentPromptAttendanceDTO is used to model the request to create a student prompt attendance record
data class StudentPromptAttendanceDTO(
    val student_identifier: String,
    val prompt_category: String,
    val prompt_name: String,
    val attendance_status: String,
    val attendance_date: String,
    val institute_id: Int,
    val start_time: String,
    val end_time: String,
    val notes: String
)
