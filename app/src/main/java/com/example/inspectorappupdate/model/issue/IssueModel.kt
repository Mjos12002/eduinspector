package com.example.inspectorappupdate.model.issue

data class IssueModel (
    val status: Int,
    val error: Boolean,
    val message: String,
)