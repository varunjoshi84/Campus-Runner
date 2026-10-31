package com.example.campusrunner.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    val nearby = runRepository.getNearbyRuns(loc.lat, loc.lng)
                    _runs.value = nearby
                }
                is LocationResult.Error -> {
                    val nearby = runRepository.getNearbyRuns(37.421999, -122.084000)
                    _runs.value = nearby
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusRunner - Nearby Runs") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (myRun == null) {
                FloatingActionButton(onClick = { showStartRunSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Start Run")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (myRun != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Your Active Run at ${myRun!!.storeName}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.endRun(myRun!!.id) }) {
                            Text("I'm Leaving / End Run")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = "Active Runs Around You", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (runs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active runs nearby. Start one or check back later!")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(runs) { run ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = run.storeName, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Runner: ${run.runnerName ?: "Campus Student"}", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { selectedRunForRequest = run }) {
                                    Text("Request")
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
        com.example.campusrunner.ui.requests.NewRequestDialog(
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
                label = { Text("Store Name (e.g. Student Union Café)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (storeName.isNotBlank()) onStart(storeName) }
            ) {
                Text("Start Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
