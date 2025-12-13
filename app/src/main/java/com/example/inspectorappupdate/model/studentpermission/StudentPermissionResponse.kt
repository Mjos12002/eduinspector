package com.example.inspectorappupdate.model.studentpermission

data class StudentPermissionResponse(
    val status: Int,
    val error: String,
    val message: String,
    val data: StudentPermissionResponseData?
)


