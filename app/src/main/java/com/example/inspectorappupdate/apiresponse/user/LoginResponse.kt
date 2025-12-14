package com.example.inspectorappupdate.apiresponse.user

data class LoginResponse(
    var status: String,
    var error: String,
    var message: String,
    var data: LoginResponseData?
) {}