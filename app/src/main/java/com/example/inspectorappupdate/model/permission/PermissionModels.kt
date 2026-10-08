package com.example.inspectorappupdate.model.permission

import com.google.gson.annotations.SerializedName

// A student allowed to leave the school for a while (visit_permissions).

data class CreatePermissionRequest(
    // reg_number or student code
    val input: String,
    @SerializedName("permission_type") val permissionType: String,
    val reason: String?,
    // yyyy-MM-dd HH:mm:ss
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String
)

data class PermissionListResponse(
    val status: Int,
    val message: String?,
    val data: PermissionPage?
)

data class PermissionPage(
    val permissions: List<Permission>?
)

data class CreatePermissionResponse(
    val status: Int,
    val message: String?
)

data class Permission(
    val id: Int,
    @SerializedName("student_name") val studentName: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("permission_type") val permissionType: String?,
    val reason: String?,
    val status: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    // When the student left the school and when they came back.
    @SerializedName("exit_time") val exitTime: String?,
    @SerializedName("entry_time") val entryTime: String?,
    @SerializedName("requested_by") val requestedBy: String?,
    // upcoming, open (may leave now), out, returned, expired or cancelled.
    @SerializedName("gate_state") val gateState: String?,
    @SerializedName("returned_late") val returnedLate: Boolean?
)

data class CancelPermissionRequest(val reason: String?)

/** The student whose card was tapped. */
data class PermissionStudent(
    val name: String,
    val regNumber: String,
    // Card status from the card lookup: only an active card can get a permission.
    val cardStatus: String
)
