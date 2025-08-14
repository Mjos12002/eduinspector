package com.example.inspectorappupdate.utils.auth

import android.util.Log
import com.example.inspectorappupdate.model.auth.CredentialsTextLengthCheck
import com.example.inspectorappupdate.viewmodel.signin.SignInViewModel


// Class used to manage user authentication
class UserAuthUtil(val username: String, val password: String) {

    // Method used to check if the credentials contain any special characters
    fun sanitizeCredentials(): CredentialsTextLengthCheck {

        // Set the patterns to check against the credentials, return false if one of fields contains one of the characters indicated in the patterns
        val strPattern = Regex("[!@#$%^&*()_+\\s]")
        val usernameMatch = strPattern.find(username)?.value
        val passwordMatch = strPattern.find(password)?.value

        var usernameSanitization = ""
        var passwordSanitization = ""
        var error = false

        // Check if username does not contain invalid characters
        if (usernameMatch != null) {
            usernameSanitization = "Invalid username"
            error = true
        }

        if (passwordMatch != null) {
            passwordSanitization = "Invalid password"
            error = true
        }
        return CredentialsTextLengthCheck(error, usernameSanitization, passwordSanitization)

    }

    fun checkCredentialsTextLength(): CredentialsTextLengthCheck {

        var usernameMsg = ""
        var passwordMsg = ""
        var error = false

        if (username.length < 4) {
            usernameMsg = "Invalid username"
            error = true
        }

        if (password.length < 4) {
            passwordMsg = "Invalid password"
            error = true
        }

        return CredentialsTextLengthCheck(error, usernameMsg, passwordMsg)
    }

    // Start the process to log the user in by checking the user credentials length then sanitization status
    fun userLoginProcess(): CredentialsTextLengthCheck {

        // Check user credentials string length
        val credentialsTextLengthStatus = checkCredentialsTextLength()
        SignInViewModel().updateCredentialStatus(credentialsTextLengthStatus)
        // Check the special characters for in the credentials
        val credentialsSpecialCharacterStatus = sanitizeCredentials()

        var response = credentialsTextLengthStatus

        if (credentialsSpecialCharacterStatus.error) {
            response = credentialsSpecialCharacterStatus
        }

        return response
    }

    fun userSignIn() {
        Log.i("LOGIN IN", "Login the user")
    }

}