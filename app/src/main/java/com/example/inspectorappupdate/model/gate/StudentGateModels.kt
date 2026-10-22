package com.example.inspectorappupdate.model.gate

import com.google.gson.annotations.SerializedName

/** A student card tapped at the gate; direction is "in" or "out". */
data class StudentGateRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("direction") val direction: String,
    @SerializedName("device_id") val deviceId: String?,
    // What the chip read on this tap (see CardChip); left out when it couldn't be read.
    @SerializedName("chip_signature") val chipSignature: String? = null,
    @SerializedName("chip_counter") val chipCounter: Int? = null
)

data class StudentGateResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: StudentGateResult?
)

/** What the server decided: whether the student may pass, why (`result`), and what was recorded. */
data class StudentGateResult(
    @SerializedName("allowed") val allowed: Boolean,
    @SerializedName("result") val result: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("student") val student: GateStudent?,
    @SerializedName("permission") val permission: GatePermission?,
    @SerializedName("attendance") val attendance: GateAttendance?
)

data class GateStudent(
    @SerializedName("name") val name: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("residence") val residence: String?
)

data class GatePermission(
    @SerializedName("type") val type: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("exit_time") val exitTime: String?,
    @SerializedName("entry_time") val entryTime: String?,
    @SerializedName("returned_late") val returnedLate: Boolean?
)

data class GateAttendance(
    @SerializedName("check_in_time") val checkInTime: String?,
    @SerializedName("check_out_time") val checkOutTime: String?,
    @SerializedName("status") val status: String?
)
