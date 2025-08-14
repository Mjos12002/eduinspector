package com.example.inspectorappupdate.utils

import android.os.Build

import androidx.annotation.RequiresApi
import com.example.inspectorappupdate.enums.HttpRequestEnums
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

object RetrofitInstance {

    private val BASE_URL = HttpRequestEnums.BASEURL.value

    private val customInterceptor = Interceptor { chain ->
        val request = chain.request()
        chain.proceed(request)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(customInterceptor)
        .callTimeout(duration = Duration.ofSeconds(4L))
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    fun getInstance(): Retrofit {
        return Retrofit
            .Builder()
            .client(okhttp)
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}