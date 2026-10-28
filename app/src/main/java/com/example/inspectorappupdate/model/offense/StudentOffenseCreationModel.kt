package com.example.inspectorappupdate.model.offense

data class StudentOffenseCreationModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: RecordedOffense? = null,
) {}

// The conduct marks the student has left this term after the fault.
data class RecordedOffense(
    val id: Int,
    val current_marks: Int? = null,
    val initial_marks: Int? = null
)