package com.example.inspectorappupdate.model.agent

import com.google.gson.annotations.SerializedName

/** The API's reply: `code` names why a card can't be issued (e.g. HAS_ACTIVE_CARD, CARD_TAKEN). */
data class AgentEnvelope<T>(
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("data") val data: T?
)

data class AgentPage<T>(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<T>?,
    @SerializedName("meta") val meta: AgentPageMeta?
)

data class AgentPageMeta(
    @SerializedName("total") val total: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int
)

/** A student at one of the agent's schools; `card_state` is "active" or "none". */
data class AgentStudent(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("student_code") val studentCode: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("institute_name") val instituteName: String?,
    @SerializedName("class_id") val classId: Int?,
    @SerializedName("class") val className: String?,
    @SerializedName("card_state") val cardState: String?,
    @SerializedName("active_card") val activeCard: AgentCard?,
    @SerializedName("print") val print: AgentPrint?,
    @SerializedName("classes") val classes: List<AgentClass>?
) {
    val hasActiveCard get() = cardState == "active"
}

data class AgentCard(
    @SerializedName("id") val id: Int,
    @SerializedName("card_number") val cardNumber: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("valid_to") val validTo: String?
)

/** The student's open print job; `with_me` when the printed card is in this agent's stock. */
data class AgentPrint(
    @SerializedName("status") val status: String?,
    @SerializedName("with_me") val withMe: Boolean,
    @SerializedName("held_by_agent") val heldByAgent: Boolean
)

data class AgentClass(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class UpdateStudentRequest(
    @SerializedName("gender") val gender: String?,
    @SerializedName("class_id") val classId: Int?
)

data class BlockCardRequest(@SerializedName("reason") val reason: String)

data class ChipCheckRequest(
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("chip_signature") val chipSignature: String? = null
)

/** Whether a tapped chip can become the student's card. */
data class ChipCheck(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)

data class IssueCardRequest(
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("card_number") val cardNumber: String,
    @SerializedName("pin") val pin: String,
    @SerializedName("pin_confirmation") val pinConfirmation: String,
    @SerializedName("chip_signature") val chipSignature: String? = null,
    @SerializedName("chip_counter") val chipCounter: Int? = null
)

data class IssueResult(
    @SerializedName("card") val card: AgentCard?,
    @SerializedName("issued_at") val issuedAt: String?,
    @SerializedName("repeated") val repeated: Boolean,
    @SerializedName("student") val student: AgentStudent?
)

data class AgentSchool(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?
)

/** Cards handed to the agent, still in hand, issued from that stock, and every card the agent activated. */
data class AgentNumbers(
    @SerializedName("institute_id") val instituteId: Int?,
    @SerializedName("handed") val handed: Int,
    @SerializedName("in_hand") val inHand: Int,
    @SerializedName("issued_from_stock") val issuedFromStock: Int,
    @SerializedName("issued") val issued: Int,
    @SerializedName("issued_today") val issuedToday: Int,
    @SerializedName("issued_7_days") val issuedWeek: Int,
    @SerializedName("schools") val schools: List<AgentNumbers>?
)

data class RecentIssue(
    @SerializedName("student") val student: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("institute_name") val instituteName: String?,
    @SerializedName("card_number") val cardNumber: String?,
    @SerializedName("from_my_stock") val fromMyStock: Boolean,
    @SerializedName("issued_at") val issuedAt: String?
)

data class AgentStatsData(
    @SerializedName("schools") val schools: List<AgentSchool>?,
    @SerializedName("stats") val stats: AgentNumbers?,
    @SerializedName("recent") val recent: List<RecentIssue>?
)

/** A printed card the agent holds, waiting for its student. */
data class StockCard(
    @SerializedName("student_id") val studentId: Int,
    @SerializedName("student") val student: String?,
    @SerializedName("reg_number") val regNumber: String?,
    @SerializedName("institute_name") val instituteName: String?,
    @SerializedName("handed_at") val handedAt: String?
)
