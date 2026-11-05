package com.example.campusrunner.ui.requests

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

enum class RequestTab {
    MY_ORDERS,      // Requests made by current student
    INCOMING_RUNS   // Requests assigned to current student as runner
}

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

    var activeTab by remember { mutableStateOf(RequestTab.MY_ORDERS) }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Open", "Accepted", "Bought", "Delivered")

    val context = LocalContext.current
    val dialPhone = { phoneNumber: String ->
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        context.startActivity(intent)
    }

    val currentList = if (activeTab == RequestTab.MY_ORDERS) mine else incoming
    val filteredRequests = currentList.filter { req ->
        when (selectedFilter.uppercase()) {
            "ALL" -> true
            "OPEN" -> req.status.uppercase() == "OPEN"
            "ACCEPTED" -> req.status.uppercase() == "ACCEPTED"
            "BOUGHT" -> req.status.uppercase() == "BOUGHT"
            "DELIVERED" -> req.status.uppercase() == "DELIVERED"
            else -> true
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orders & Requests",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFEA580C))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab Switcher: "My Orders (Sent)" vs "Incoming (As Runner)"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == RequestTab.MY_ORDERS) Color.White else Color.Transparent)
                        .clickable { activeTab = RequestTab.MY_ORDERS },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "My Orders (${mine.size})",
                        fontWeight = if (activeTab == RequestTab.MY_ORDERS) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == RequestTab.MY_ORDERS) Color(0xFFEA580C) else Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == RequestTab.INCOMING_RUNS) Color.White else Color.Transparent)
                        .clickable { activeTab = RequestTab.INCOMING_RUNS },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Runner Tasks (${incoming.size})",
                        fontWeight = if (activeTab == RequestTab.INCOMING_RUNS) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == RequestTab.INCOMING_RUNS) Color(0xFFEA580C) else Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Color(0xFFEA580C) else Color(0xFFF3F4F6),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEA580C))
                }
            } else if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (activeTab == RequestTab.MY_ORDERS) "🛍️" else "🏃‍♂️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (activeTab == RequestTab.MY_ORDERS)
                                "No orders found in this filter."
                            else
                                "No incoming delivery tasks right now.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests) { req ->
                        RequestCard(
                            req = req,
                            isRunnerView = activeTab == RequestTab.INCOMING_RUNS,
                            onUpdateStatus = { status -> viewModel.updateStatus(req.id, status) },
                            onCall = { phone -> dialPhone(phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    req: RequestDto,
    isRunnerView: Boolean,
    onUpdateStatus: (String) -> Unit,
    onCall: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Item Name, Store, and Tip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📦", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = req.item,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Store: ${req.storeName ?: "Campus Shop"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED)
                ) {
                    Text(
                        text = "+₹${req.tip} Tip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFEA580C),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Row: Peer Identity & Phone (if unlocked)
            val peerName = if (isRunnerView) (req.requesterName ?: "Requester") else (req.runnerName ?: "Runner")
            val peerPhone = if (isRunnerView) req.requesterPhone else req.runnerPhone
            val isAcceptedOrLater = req.status.uppercase() in listOf("ACCEPTED", "BOUGHT", "DELIVERED")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRunnerView) "👤 Ordered by: " else "🏃 Runner: ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = peerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                StatusChip(status = req.status)
            }

            // Phone Contact Action (Unlocked after acceptance)
            if (isAcceptedOrLater && !peerPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📞 $peerPhone", fontSize = 13.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { onCall(peerPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Buttons Section
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            if (isRunnerView) {
                // Runner actions: Accept, Buy, Deliver
                when (req.status.uppercase()) {
                    "OPEN" -> {
                        Button(
                            onClick = { onUpdateStatus("ACCEPTED") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accept Request (+₹${req.tip})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "ACCEPTED" -> {
                        Button(
                            onClick = { onUpdateStatus("BOUGHT") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark as Bought", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "BOUGHT" -> {
                        Button(
                            onClick = { onUpdateStatus("DELIVERED") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Delivered", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "DELIVERED" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅ Order completed & delivered", fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                        }
                    }
                    "CANCELLED" -> {
                        Text("❌ Request cancelled", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            } else {
                // Buyer actions: Cancel when Open
                when (req.status.uppercase()) {
                    "OPEN" -> {
                        OutlinedButton(
                            onClick = { onUpdateStatus("CANCELLED") },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Request", fontSize = 12.sp)
                        }
                    }
                    "ACCEPTED" -> {
                        Text("🏃 Runner has accepted! Awaiting purchase.", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    "BOUGHT" -> {
                        Text("🛍️ Item bought! Runner is on their way.", fontSize = 12.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    "DELIVERED" -> {
                        Text("🎉 Delivered! Remember to pay the tip to runner.", fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    "CANCELLED" -> {
                        Text("Request was cancelled", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}
