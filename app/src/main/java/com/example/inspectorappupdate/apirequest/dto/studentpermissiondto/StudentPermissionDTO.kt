package com.example.inspectorappupdate.apirequest.dto.studentpermissiondto

data class StudentPermissionDTO(
    val input: String,
    val institute_id: Int,
    val permission_type: String,
    val reason: String,
    val start_time: String,
    val end_time: String
)
