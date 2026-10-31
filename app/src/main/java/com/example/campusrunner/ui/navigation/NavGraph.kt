package com.example.campusrunner.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.campusrunner.AppContainer
import com.example.campusrunner.ui.auth.LoginScreen
import com.example.campusrunner.ui.home.HomeScreen
import com.example.campusrunner.ui.requests.RequestsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Requests : Screen("requests", "Requests", Icons.Default.List)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Login : Screen("login", "Login", Icons.Default.Person)
}

@Composable
fun NavGraph(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val isLoggedIn = container.authRepository.isLoggedIn
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    val items = listOf(
        Screen.Home,
        Screen.Requests,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Screen.Login.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    runRepository = container.runRepository,
                    requestRepository = container.requestRepository,
                    locationClient = container.locationClient
                )
            }
            composable(Screen.Requests.route) {
                RequestsScreen(
                    requestRepository = container.requestRepository
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    container = container,
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    authRepository = container.authRepository,
                    onLoggedIn = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    container: AppContainer,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf(container.tokenStore.userPhone ?: "") }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Profile", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Email: ${container.tokenStore.userEmail ?: "N/A"}")
            Text(text = "Name: ${container.tokenStore.userName ?: "N/A"}")
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (for runners)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        val res = container.authRepository.updatePhone(phone)
                        if (res.isSuccess) {
                            message = "Phone updated successfully!"
                        } else {
                            message = "Failed to update phone"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Phone Number")
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    container.authRepository.logout()
                    onLoggedOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out")
            }
        }
    }
}
