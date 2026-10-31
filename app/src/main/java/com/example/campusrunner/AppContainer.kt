package com.example.campusrunner

import android.content.Context
import com.example.campusrunner.data.TokenStore
import com.example.campusrunner.data.local.AppDatabase
import com.example.campusrunner.data.remote.ApiService
import com.example.campusrunner.data.remote.AuthInterceptor
import com.example.campusrunner.data.repository.AuthRepository
import com.example.campusrunner.data.repository.RequestRepository
import com.example.campusrunner.data.repository.RunRepository
import com.example.campusrunner.location.LocationClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)

    private val authInterceptor = AuthInterceptor(tokenStore)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    private val database = AppDatabase()

    val authRepository = AuthRepository(apiService, tokenStore)
    val runRepository = RunRepository(apiService, database)
    val requestRepository = RequestRepository(apiService, database)
    val locationClient = LocationClient(context)
}
