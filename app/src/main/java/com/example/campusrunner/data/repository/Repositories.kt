package com.example.campusrunner.data.repository

import com.example.campusrunner.data.TokenStore
import com.example.campusrunner.data.local.AppDatabase
import com.example.campusrunner.data.local.RequestEntity
import com.example.campusrunner.data.local.RunEntity
import com.example.campusrunner.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun customSignup(name: String, email: String, password: String, phone: String?): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.signup(SignupRequest(name, email, password, phone))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.token = body.token
                tokenStore.userId = body.user.id
                tokenStore.userName = body.user.name
                tokenStore.userEmail = body.user.email
                tokenStore.userPhone = body.user.phone
                Result.success(body.user)
            } else {
                val errorMsg = try {
                    val errorBody = response.errorBody()?.string()
                    org.json.JSONObject(errorBody ?: "").optString("error", "Sign up failed")
                } catch (e: Exception) {
                    "Sign up failed"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun customLogin(email: String, password: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(CustomLoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.token = body.token
                tokenStore.userId = body.user.id
                tokenStore.userName = body.user.name
                tokenStore.userEmail = body.user.email
                tokenStore.userPhone = body.user.phone
                Result.success(body.user)
            } else {
                val errorMsg = try {
                    val errorBody = response.errorBody()?.string()
                    org.json.JSONObject(errorBody ?: "").optString("error", "Invalid email or password")
                } catch (e: Exception) {
                    "Invalid email or password"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun googleLogin(idToken: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.googleLogin(GoogleLoginRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.token = body.token
                tokenStore.userId = body.user.id
                tokenStore.userName = body.user.name
                tokenStore.userEmail = body.user.email
                tokenStore.userPhone = body.user.phone
                Result.success(body.user)
            } else {
                Result.failure(Exception("Google login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun devLogin(email: String, name: String?): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.devLogin(DevLoginRequest(name, email))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.token = body.token
                tokenStore.userId = body.user.id
                tokenStore.userName = body.user.name
                tokenStore.userEmail = body.user.email
                tokenStore.userPhone = body.user.phone
                Result.success(body.user)
            } else {
                Result.failure(Exception("Dev login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                tokenStore.userPhone = user.phone
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhone(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updatePhone(UpdatePhoneRequest(phone))
            if (response.isSuccessful) {
                tokenStore.userPhone = phone
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update phone"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStore.clear()
    }

    val isLoggedIn: Boolean
        get() = !tokenStore.token.isNullOrBlank()
}

class RunRepository(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    suspend fun getNearbyRuns(lat: Double, lng: Double): List<RunDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNearbyRuns(lat, lng)
            if (response.isSuccessful && response.body() != null) {
                val runs = response.body()!!
                database.runDao().clearRuns()
                database.runDao().insertRuns(runs.map {
                    RunEntity(it.id, it.runnerId, it.storeName, it.lat, it.lng, it.expiresAt, it.runnerName)
                })
                runs
            } else {
                database.runDao().getAllRuns().map {
                    RunDto(it.id, it.runnerId, it.storeName, it.lat, it.lng, it.expiresAt, it.runnerName)
                }
            }
        } catch (e: Exception) {
            database.runDao().getAllRuns().map {
                RunDto(it.id, it.runnerId, it.storeName, it.lat, it.lng, it.expiresAt, it.runnerName)
            }
        }
    }

    suspend fun getMyActiveRun(): RunDto? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyActiveRun()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createRun(storeName: String, lat: Double, lng: Double): Result<RunDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createRun(CreateRunRequest(storeName, lat, lng))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start run"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endRun(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.endRun(id)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to end run"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RequestRepository(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    suspend fun createRequest(runId: String, item: String, tip: Int): Result<RequestDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createRequest(CreateRequestRequest(runId, item, tip))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIncomingRequests(): List<RequestDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIncomingRequests()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMyRequests(): List<RequestDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyRequests()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateStatus(id: String, status: String): Result<RequestDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateRequestStatus(id, UpdateStatusRequest(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
