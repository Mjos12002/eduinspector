package com.example.inspectorappupdate.model.visitor

import com.google.gson.annotations.SerializedName

data class GateScanRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("device_id") val deviceId: String?
)

// Visitors (external_visitors.id) the gate staff marked as coming in, or everyone inside checking out.
data class GateRecordRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("check_in") val checkIn: List<Int>,
    @SerializedName("check_out_all") val checkOutAll: Boolean = false
)

data class AttachCardRequest(
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("chip_type") val chipType: String?
)

data class GateStateResponse(
    val status: Int,
    val error: Boolean,
    val message: String?,
    val data: GateState?
)

// A tapped visitor card: the student and every authorized visitor, with who is inside. After recording,
// result is "accepted" and checked_in / checked_out name who was recorded.
data class GateState(
    // visitor | student | unknown
    val type: String?,
    val authorized: Boolean?,
    val reason: String?,
    val message: String?,
    val uid: String?,
    val card: GateCard?,
    val student: GateStudent?,
    val visitors: List<GateVisitor>?,
    // accepted | refused, only after recording
    val result: String?,
    @SerializedName("checked_in") val checkedIn: List<String>?,
    @SerializedName("checked_out") val checkedOut: List<String>?
)

data class GateCard(
    val id: Int,
    @SerializedName("card_code") val cardCode: String?,
    val status: String?
)

data class GateStudent(
    @SerializedName("student_id") val studentId: Int,
    val name: String?,
    @SerializedName("reg_number") val regNumber: String?
)

data class GateVisitor(
    val id: Int,
    @SerializedName("full_name") val fullName: String?,
    val phone: String?,
    val relationship: String?,
    @SerializedName("is_enabled") val isEnabled: Boolean?,
    val inside: Boolean?,
    @SerializedName("check_in_time") val checkInTime: String?
)

data class VisitorCardLookupResponse(
    val status: Int,
    val error: Boolean,
    val message: String?,
    val data: List<VisitorCardItem>?
)

data class VisitorCardResponse(
    val status: Int,
    val error: Boolean,
    val message: String?,
    val data: VisitorCardItem?
)

/** A student's visitor card, for attaching the physical card. */
data class VisitorCardItem(
    val id: Int,
    @SerializedName("card_code") val cardCode: String?,
    // pending | active | inactive
    val status: String?,
    @SerializedName("chip_attached") val chipAttached: Boolean?,
    val student: CardStudent?,
    val visitors: List<CardVisitor>?
)

data class CardStudent(
    val id: Int,
    val name: String?,
    @SerializedName("reg_number") val regNumber: String?
)

data class CardVisitor(
    val id: Int,
    @SerializedName("full_name") val fullName: String?,
    val relationship: String?,
    @SerializedName("is_enabled") val isEnabled: Boolean?
)

data class RegisterStudentResponse(
    val status: Int,
    val message: String?,
    val data: RegisterStudent?
)

/** The student whose card was tapped on Register visitor, with the visitors already allowed to visit them. */
data class RegisterStudent(
    @SerializedName("student_id") val studentId: Int,
    val name: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("card_status") val cardStatus: String?,
    @SerializedName("visitor_card") val visitorCard: GateCard?,
    val visitors: List<GateVisitor>?
)

data class RegisterVisitorRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    val students: List<RegisterVisitorStudent>
)

data class RegisterVisitorStudent(
    @SerializedName("student_id") val studentId: Int,
    val relationship: String
)

data class RegisterVisitorResponse(
    val status: Int,
    val message: String?
)
