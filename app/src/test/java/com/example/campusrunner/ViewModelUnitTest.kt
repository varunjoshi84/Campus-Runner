package com.example.campusrunner

import com.example.campusrunner.data.TokenStore
import com.example.campusrunner.data.local.AppDatabase
import com.example.campusrunner.data.remote.*
import com.example.campusrunner.data.repository.AuthRepository
import com.example.campusrunner.data.repository.RequestRepository
import com.example.campusrunner.data.repository.RunRepository
import com.example.campusrunner.location.LocationClient
import com.example.campusrunner.location.LocationResult
import com.example.campusrunner.ui.home.HomeViewModel
import com.example.campusrunner.ui.requests.RequestsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

// Fake ApiService for deterministic, hermetic unit tests
class FakeApiService : ApiService {
    var nearbyRunsToReturn = mutableListOf<RunDto>()
    var myActiveRunToReturn: RunDto? = null
    var incomingRequestsToReturn = mutableListOf<RequestDto>()
    var myRequestsToReturn = mutableListOf<RequestDto>()
    var shouldFail = false

    override suspend fun signup(request: SignupRequest): Response<AuthResponse> {
        return if (shouldFail) {
            Response.error(400, "{}".toResponseBody("application/json".toMediaType()))
        } else {
            Response.success(AuthResponse("fake_token_123", UserDto("u1", request.name, request.email, request.phone)))
        }
    }

    override suspend fun login(request: CustomLoginRequest): Response<AuthResponse> {
        return if (shouldFail) {
            Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
        } else {
            Response.success(AuthResponse("fake_token_123", UserDto("u1", "Test User", request.email, "9876543210")))
        }
    }

    override suspend fun googleLogin(request: GoogleLoginRequest): Response<AuthResponse> {
        return Response.success(AuthResponse("token_g", UserDto("u1", "Google User", "g@x.com", null)))
    }

    override suspend fun devLogin(request: DevLoginRequest): Response<AuthResponse> {
        return Response.success(AuthResponse("token_d", UserDto("u1", request.name ?: "Dev", request.email, null)))
    }

    override suspend fun getProfile(): Response<UserDto> {
        return Response.success(UserDto("u1", "User", "u@x.com", "9876543210"))
    }

    override suspend fun updatePhone(request: UpdatePhoneRequest): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun getNearbyRuns(lat: Double, lng: Double): Response<List<RunDto>> {
        return Response.success(nearbyRunsToReturn)
    }

    override suspend fun getMyActiveRun(): Response<RunDto?> {
        return Response.success(myActiveRunToReturn)
    }

    override suspend fun createRun(request: CreateRunRequest): Response<RunDto> {
        val run = RunDto("new_run_1", "runner_1", request.storeName, request.lat, request.lng, "2026-11-01T23:00:00Z", "Runner A")
        myActiveRunToReturn = run
        return Response.success(run)
    }

    override suspend fun endRun(id: String): Response<Unit> {
        myActiveRunToReturn = null
        return Response.success(Unit)
    }

    override suspend fun createRequest(request: CreateRequestRequest): Response<RequestDto> {
        val req = RequestDto("req_1", request.runId, "u_buyer", request.item, request.tip, "OPEN", "2026-11-01T22:30:00Z", "Campus Mart", "Requester", null, null, null)
        myRequestsToReturn.add(req)
        return Response.success(req)
    }

    override suspend fun getIncomingRequests(): Response<List<RequestDto>> {
        return Response.success(incomingRequestsToReturn)
    }

    override suspend fun getMyRequests(): Response<List<RequestDto>> {
        return Response.success(myRequestsToReturn)
    }

    override suspend fun updateRequestStatus(id: String, request: UpdateStatusRequest): Response<RequestDto> {
        val updated = RequestDto(id, "r1", "u1", "Item", 10, request.status, "now", "Store", "A", null, "B", "9999999999")
        return Response.success(updated)
    }
}

class FakeLocationClient : LocationClient {
    var locationToReturn: LocationResult = LocationResult.Success(31.256, 75.705)
    override suspend fun getCurrentLocation(): LocationResult = locationToReturn
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelUnitTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApi: FakeApiService
    private lateinit var fakeLocation: FakeLocationClient
    private lateinit var tokenStore: TokenStore
    private lateinit var authRepository: AuthRepository
    private lateinit var runRepository: RunRepository
    private lateinit var requestRepository: RequestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeApiService()
        fakeLocation = FakeLocationClient()
        tokenStore = TokenStore()
        authRepository = AuthRepository(fakeApi, tokenStore)
        val database = AppDatabase()
        runRepository = RunRepository(fakeApi, database)
        requestRepository = RequestRepository(fakeApi, database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authRepository_customSignupAndLogout() = runTest(testDispatcher) {
        val res = authRepository.customSignup("Varun", "v@lpu.in", "pass123", "9876543210")
        assertTrue(res.isSuccess)
        assertEquals("fake_token_123", tokenStore.token)
        assertTrue(authRepository.isLoggedIn)

        authRepository.logout()
        assertNull(tokenStore.token)
        assertFalse(authRepository.isLoggedIn)
    }

    @Test
    fun homeViewModel_loadsNearbyRunsAndActiveRun() = runTest(testDispatcher) {
        fakeApi.nearbyRunsToReturn.add(
            RunDto("run_1", "r1", "Stationery Shop", 31.256, 75.705, "2026-11-01T23:00:00Z", "Runner Bob")
        )
        fakeApi.myActiveRunToReturn = RunDto("my_run_1", "me", "Campus Canteen", 31.256, 75.705, "2026-11-01T23:00:00Z", "Me")

        val viewModel = HomeViewModel(runRepository, requestRepository, fakeLocation)
        advanceUntilIdle()

        assertEquals(1, viewModel.runs.value.size)
        assertEquals("Stationery Shop", viewModel.runs.value[0].storeName)
        assertNotNull(viewModel.myRun.value)
        assertEquals("Campus Canteen", viewModel.myRun.value?.storeName)
    }

    @Test
    fun homeViewModel_startAndEndRun() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(runRepository, requestRepository, fakeLocation)
        advanceUntilIdle()

        assertNull(viewModel.myRun.value)

        var started = false
        viewModel.startRun("Pharmacy") { started = true }
        advanceUntilIdle()

        assertTrue(started)
        assertEquals("Pharmacy", viewModel.myRun.value?.storeName)

        viewModel.endRun(viewModel.myRun.value!!.id)
        advanceUntilIdle()

        assertNull(viewModel.myRun.value)
    }

    @Test
    fun requestsViewModel_loadsAndUpdatesStatus() = runTest(testDispatcher) {
        fakeApi.incomingRequestsToReturn.add(
            RequestDto("req_in_1", "run_1", "u2", "Notebook", 20, "OPEN", "now", "Stationery", "Requester Rahul", "9876543210", null, null)
        )
        fakeApi.myRequestsToReturn.add(
            RequestDto("req_my_1", "run_2", "me", "Juice", 15, "ACCEPTED", "now", "Canteen", null, null, "Runner Amit", "9123456780")
        )

        val viewModel = RequestsViewModel(requestRepository)
        advanceUntilIdle()

        assertEquals(1, viewModel.incomingRequests.value.size)
        assertEquals("Notebook", viewModel.incomingRequests.value[0].item)
        assertEquals(1, viewModel.myRequests.value.size)
        assertEquals("Juice", viewModel.myRequests.value[0].item)

        viewModel.updateStatus("req_in_1", "ACCEPTED")
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }
}

