package com.example.inspectorappupdate.enums

import com.example.inspectorappupdate.BuildConfig

enum class HttpRequestEnums(val value: String = "") {
    // Set per build type in app/build.gradle.kts.
    BASEURL(BuildConfig.API_BASE_URL),
    SECUREPROTOCOL("https"),
    INSECUREPROTOCOL("http")
}