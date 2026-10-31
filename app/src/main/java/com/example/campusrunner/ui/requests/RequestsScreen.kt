package com.example.campusrunner.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusrunner.data.remote.RequestDto
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

@Composable
fun RequestsScreen(
    requestRepository: RequestRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: RequestsViewModel = viewModel(factory = RequestsViewModel.Factory(requestRepository))
    val incoming by viewModel.incomingRequests.collectAsState()
    val mine by viewModel.myRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Open", "Accepted", "Bought")

    val filteredRequests = mine.filter { req ->
        when (selectedFilter.uppercase()) {
            "ALL" -> true
            "OPEN" -> req.status.uppercase() == "OPEN"
            "ACCEPTED" -> req.status.uppercase() == "ACCEPTED"
            "BOUGHT" -> req.status.uppercase() == "BOUGHT"
            else -> true
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "My Requests", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFEA580C) else Color(0xFFF3F4F6),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else if (filteredRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No requests found.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRequests) { req ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📦", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = req.item, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Today, 4:20 PM · At: ${req.storeName ?: "Campus Store"}", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        StatusChip(status = req.status)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "₹${req.tip}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFEA580C))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
