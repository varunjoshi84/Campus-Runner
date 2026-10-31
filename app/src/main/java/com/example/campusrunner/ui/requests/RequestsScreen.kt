package com.example.campusrunner.ui.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusrunner.data.remote.RequestDto
import com.example.campusrunner.data.remote.RunDto
import com.example.campusrunner.data.repository.RequestRepository
import com.example.campusrunner.ui.components.StatusChip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RequestsViewModel(private val requestRepository: RequestRepository) : ViewModel() {
    private val _incomingRequests = MutableStateFlow<List<RequestDto>>(emptyList())
    val incomingRequests: StateFlow<List<RequestDto>> = _incomingRequests.asStateFlow()

    private val _myRequests = MutableStateFlow<List<RequestDto>>(emptyList())
    val myRequests: StateFlow<List<RequestDto>> = _myRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _incomingRequests.value = requestRepository.getIncomingRequests()
            _myRequests.value = requestRepository.getMyRequests()
            _isLoading.value = false
        }
    }

    fun updateStatus(id: String, status: String) {
        viewModelScope.launch {
            val res = requestRepository.updateStatus(id, status)
            if (res.isSuccess) {
                refresh()
            }
        }
    }

    class Factory(private val requestRepository: RequestRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RequestsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RequestsViewModel(requestRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    requestRepository: RequestRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: RequestsViewModel = viewModel(factory = RequestsViewModel.Factory(requestRepository))
    val incoming by viewModel.incomingRequests.collectAsState()
    val mine by viewModel.myRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CampusRunner - Requests") })
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Incoming (${incoming.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Requests (${mine.size})") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedTab == 0) {
                    if (incoming.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No incoming requests for your runs.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(incoming) { req ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = req.item, style = MaterialTheme.typography.titleMedium)
                                            StatusChip(status = req.status)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Store: ${req.storeName ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Requester: ${req.requesterName ?: "Student"} | Tip: \$${req.tip}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (req.status == "OPEN") {
                                                Button(onClick = { viewModel.updateStatus(req.id, "ACCEPTED") }) {
                                                    Text("Accept")
                                                }
                                            }
                                            if (req.status == "ACCEPTED") {
                                                Button(onClick = { viewModel.updateStatus(req.id, "BOUGHT") }) {
                                                    Text("Bought")
                                                }
                                            }
                                            if (req.status == "BOUGHT") {
                                                Button(onClick = { viewModel.updateStatus(req.id, "DELIVERED") }) {
                                                    Text("Delivered")
                                                }
                                            }
                                            if (req.status != "DELIVERED" && req.status != "CANCELLED") {
                                                OutlinedButton(onClick = { viewModel.updateStatus(req.id, "CANCELLED") }) {
                                                    Text("Cancel")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (mine.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("You haven't requested anything yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mine) { req ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = req.item, style = MaterialTheme.typography.titleMedium)
                                            StatusChip(status = req.status)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Store: ${req.storeName ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Runner: ${req.runnerName ?: "Student"} | Tip: \$${req.tip}", style = MaterialTheme.typography.bodySmall)
                                        if (req.status != "DELIVERED" && req.status != "CANCELLED") {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(onClick = { viewModel.updateStatus(req.id, "CANCELLED") }) {
                                                Text("Cancel Request")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewRequestDialog(
    run: RunDto,
    onDismiss: () -> Unit,
    onSubmit: (String, Int) -> Unit
) {
    var item by remember { mutableStateOf("") }
    var tipStr by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request from ${run.storeName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item description (e.g. Iced Latte)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tipStr,
                    onValueChange = { tipStr = it },
                    label = { Text("Tip ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tip = tipStr.toIntOrNull() ?: 0
                    if (item.isNotBlank()) {
                        onSubmit(item, tip)
                    }
                }
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
