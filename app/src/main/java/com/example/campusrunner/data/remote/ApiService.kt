package com.example.campusrunner.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/dev")
    suspend fun devLogin(@Body request: DevLoginRequest): Response<AuthResponse>

    @GET("users/me")
    suspend fun getProfile(): Response<UserDto>

    @PATCH("users/me")
    suspend fun updatePhone(@Body request: UpdatePhoneRequest): Response<Unit>

    @GET("runs")
    suspend fun getNearbyRuns(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<RunDto>>

    @GET("runs/mine")
    suspend fun getMyActiveRun(): Response<RunDto?>

    @POST("runs")
    suspend fun createRun(@Body request: CreateRunRequest): Response<RunDto>

    @POST("runs/{id}/end")
    suspend fun endRun(@Path("id") id: String): Response<Unit>

    @POST("requests")
    suspend fun createRequest(@Body request: CreateRequestRequest): Response<RequestDto>

    @GET("requests/incoming")
    suspend fun getIncomingRequests(): Response<List<RequestDto>>

    @GET("requests/mine")
    suspend fun getMyRequests(): Response<List<RequestDto>>

    @PATCH("requests/{id}")
    suspend fun updateRequestStatus(@Path("id") id: String, @Body request: UpdateStatusRequest): Response<RequestDto>
}
