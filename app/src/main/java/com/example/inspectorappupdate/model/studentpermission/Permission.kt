package com.example.inspectorappupdate.model.studentpermission

data class Permission(
    val id: Int,
    val reason: String,
    val status: String,
    val start_time: String,
    val end_time: String,
    val has_exited: String
)
