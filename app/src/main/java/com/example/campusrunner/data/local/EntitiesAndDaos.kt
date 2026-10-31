package com.example.campusrunner.data.local

data class RunEntity(
    val id: String,
    val runnerId: String?,
    val storeName: String,
    val lat: Double,
    val lng: Double,
    val expiresAt: String,
    val runnerName: String?
)

data class RequestEntity(
    val id: String,
    val runId: String,
    val requesterId: String?,
    val item: String,
    val tip: Int,
    val status: String,
    val createdAt: String,
    val storeName: String?,
    val requesterName: String?,
    val requesterPhone: String?,
    val runnerName: String?,
    val runnerPhone: String?
)

interface RunDao {
    suspend fun getAllRuns(): List<RunEntity>
    suspend fun insertRuns(runs: List<RunEntity>)
    suspend fun clearRuns()
}

interface RequestDao {
    suspend fun getAllRequests(): List<RequestEntity>
    suspend fun insertRequests(requests: List<RequestEntity>)
    suspend fun clearRequests()
}

class AppDatabase {
    private val runs = mutableListOf<RunEntity>()
    private val requests = mutableListOf<RequestEntity>()

    private val runDaoImpl = object : RunDao {
        override suspend fun getAllRuns(): List<RunEntity> = runs.toList()
        override suspend fun insertRuns(runs: List<RunEntity>) {
            this@AppDatabase.runs.clear()
            this@AppDatabase.runs.addAll(runs)
        }
        override suspend fun clearRuns() { this@AppDatabase.runs.clear() }
    }

    private val requestDaoImpl = object : RequestDao {
        override suspend fun getAllRequests(): List<RequestEntity> = requests.toList()
        override suspend fun insertRequests(requests: List<RequestEntity>) {
            this@AppDatabase.requests.clear()
            this@AppDatabase.requests.addAll(requests)
        }
        override suspend fun clearRequests() { this@AppDatabase.requests.clear() }
    }

    fun runDao(): RunDao = runDaoImpl
    fun requestDao(): RequestDao = requestDaoImpl
}
