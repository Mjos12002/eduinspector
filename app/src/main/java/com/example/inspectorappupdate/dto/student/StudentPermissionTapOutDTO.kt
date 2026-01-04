package com.example.inspectorappupdate.dto.student

// StudentPermissionTapOutDTO is used to model the DTO for student permission tap out
data class StudentPermissionTapOutDTO(
    val card_number: String,
    val device_id: String,
    val visit_permission_id: Int
)
