package com.example.inspectorappupdate.model.studentpermission

data class StudentPermissionResponseData(
    val student: Student,
    val permissions: List<Permission>
)
