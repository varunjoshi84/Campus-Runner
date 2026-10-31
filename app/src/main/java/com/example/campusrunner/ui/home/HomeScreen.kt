package com.example.campusrunner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusrunner.data.remote.RunDto
import com.example.campusrunner.data.repository.RequestRepository
import com.example.campusrunner.data.repository.RunRepository
import com.example.campusrunner.location.LocationClient
import com.example.campusrunner.location.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val runRepository: RunRepository,
    private val requestRepository: RequestRepository,
    private val locationClient: LocationClient
) : ViewModel() {
    private val _runs = MutableStateFlow<List<RunDto>>(emptyList())
    val runs: StateFlow<List<RunDto>> = _runs.asStateFlow()

    private val _myRun = MutableStateFlow<RunDto?>(null)
    val myRun: StateFlow<RunDto?> = _myRun.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val loc = locationClient.getCurrentLocation()) {
                is LocationResult.Success -> {
                    _runs.value = runRepository.getNearbyRuns(loc.lat, loc.lng)
                }
                is LocationResult.Error -> {
                    _runs.value = runRepository.getNearbyRuns(37.421999, -122.084000)
                }
            }
            _myRun.value = runRepository.getMyActiveRun()
            _isLoading.value = false
        }
    }

    fun startRun(storeName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val loc = locationClient.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val res = runRepository.createRun(storeName, loc.lat, loc.lng)
                    if (res.isSuccess) {
                        _myRun.value = res.getOrNull()
                        refreshData()
                        onSuccess()
                    }
                }
                else -> {}
            }
        }
    }

    fun endRun(runId: String) {
        viewModelScope.launch {
            val res = runRepository.endRun(runId)
            if (res.isSuccess) {
                _myRun.value = null
                refreshData()
            }
        }
    }

    fun requestItem(runId: String, item: String, tip: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = requestRepository.createRequest(runId, item, tip)
            onResult(res.isSuccess)
        }
    }

    class Factory(
        private val runRepository: RunRepository,
        private val requestRepository: RequestRepository,
        private val locationClient: LocationClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(runRepository, requestRepository, locationClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@Composable
fun HomeScreen(
    runRepository: RunRepository,
    requestRepository: RequestRepository,
    locationClient: LocationClient,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(runRepository, requestRepository, locationClient)
    )
    val runs by viewModel.runs.collectAsState()
    val myRun by viewModel.myRun.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showStartRunSheet by remember { mutableStateOf(false) }
    var selectedRunForRequest by remember { mutableStateOf<RunDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Canteen", "Stationery", "Pharmacy", "Market")

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Greeting Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA580C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👨‍🎓", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Hi, Varun ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "👋", fontSize = 14.sp)
                        }
                        Text(text = "LPU Campus", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search stores, items...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedBorderColor = Color(0xFFEA580C)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFEA580C) else Color(0xFFF3F4F6),
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "I'm at a Store" Card
            if (myRun == null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "I'm at a Store", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Let others request items from you", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showStartRunSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Start a Run ➔", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Text(text = "🏃‍♂️", fontSize = 48.sp)
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Your Active Run at ${myRun!!.storeName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E40AF))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.endRun(myRun!!.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("I'm Leaving / End Run", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Runs Nearby Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Runs Nearby (${runs.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "View All",
                    color = Color(0xFFEA580C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { viewModel.refreshData() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else if (runs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No active runs nearby. Start one or check back later!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(runs) { run ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "👤", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = run.runnerName ?: "Kamal S.", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "At: ${run.storeName}", fontSize = 13.sp, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "🟢 150m away · 12 min left", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Button(
                                    onClick = { selectedRunForRequest = run },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED)),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Request", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartRunSheet) {
        StartRunSheet(
            onDismiss = { showStartRunSheet = false },
            onStart = { storeName ->
                viewModel.startRun(storeName) {
                    showStartRunSheet = false
                }
            }
        )
    }

    if (selectedRunForRequest != null) {
        com.example.campusrunner.ui.requests.RequestDetailScreen(
            run = selectedRunForRequest!!,
            onDismiss = { selectedRunForRequest = null },
            onSubmit = { item, tip ->
                viewModel.requestItem(selectedRunForRequest!!.id, item, tip) { success ->
                    if (success) selectedRunForRequest = null
                }
            }
        )
    }
}

@Composable
fun StartRunSheet(
    onDismiss: () -> Unit,
    onStart: (String) -> Unit
) {
    var storeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a Run") },
        text = {
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Store Name (e.g. Campus Store)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (storeName.isNotBlank()) onStart(storeName) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
