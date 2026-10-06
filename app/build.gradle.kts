plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.inspectorappupdate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.inspectorappupdate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Backend origin. Override for a local backend with -PeducardApiUrl=http://<lan-ip>:8000/
    val apiUrlOverride = project.findProperty("educardApiUrl") as String?

    buildTypes {
        debug {
            // Installs next to the production app on the same phone.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", "\"${apiUrlOverride ?: "https://test-api.educard.rw/"}\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${apiUrlOverride ?: "https://api.educard.rw/"}\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    val room_version = "2.7.2"
    implementation(libs.gson)
    implementation(libs.data.store)
    implementation(libs.room.runtime)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava)
    implementation(libs.logging.interceptor)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.rxandroid)
    implementation(libs.picasso)
    implementation(libs.material.dialogs)
    implementation(libs.legacy.support)
    implementation(libs.zxing)
    implementation(libs.annotation)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    ksp("androidx.room:room-compiler:$room_version")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}