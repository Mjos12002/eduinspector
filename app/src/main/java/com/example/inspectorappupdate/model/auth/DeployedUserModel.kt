package com.example.inspectorappupdate.model.auth

data class DeployedUserModel(
    val status: Int,
    val error: Boolean,
    val message: String,
    val data: DeployedUserModelData?
)
