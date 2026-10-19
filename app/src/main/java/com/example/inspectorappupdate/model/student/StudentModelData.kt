package com.example.inspectorappupdate.model.student

// id is the account (users.id); student.id is the student record that discipline uses.
data class StudentModelData(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val reg_number: String,
    val student: StudentRecordRef? = null
)

data class StudentRecordRef(
    val id: Int,
    val current_institute_id: Int?
)
