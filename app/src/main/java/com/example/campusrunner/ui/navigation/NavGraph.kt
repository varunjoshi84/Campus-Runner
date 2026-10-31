package com.example.campusrunner.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.campusrunner.AppContainer
import com.example.campusrunner.ui.auth.LoginScreen
import com.example.campusrunner.ui.home.HomeScreen
import com.example.campusrunner.ui.onboarding.OnboardingScreen
import com.example.campusrunner.ui.requests.RequestsScreen
import com.example.campusrunner.ui.splash.SplashScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Login : Screen("login", "Login", Icons.Default.Person)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Requests : Screen("requests", "Requests", Icons.Default.List)
    object Runs : Screen("runs", "Runs", Icons.Default.DirectionsRun)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun NavGraph(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val isLoggedIn = container.authRepository.isLoggedIn
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Splash.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Requests,
        Screen.Runs,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val showBottomNav = currentRoute in bottomNavItems.map { it.route }

            if (showBottomNav) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFEA580C),
                                selectedTextColor = Color(0xFFEA580C),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color(0xFFFFF7ED)
                            ),
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
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onSkip = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onNext = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
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
            composable(Screen.Runs.route) {
                // Runs tab reuses HomeScreen active runs or active run view
                HomeScreen(
                    runRepository = container.runRepository,
                    requestRepository = container.requestRepository,
                    locationClient = container.locationClient
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
        }
    }
}

data class ProfileMenuItem(val title: String, val icon: ImageVector)

@Composable
fun ProfileScreen(
    container: AppContainer,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        ProfileMenuItem("My Runs", Icons.Default.DirectionsRun),
        ProfileMenuItem("My Requests", Icons.Default.List),
        ProfileMenuItem("Saved Stores", Icons.Default.Bookmark),
        ProfileMenuItem("Settings", Icons.Default.Settings),
        ProfileMenuItem("Help & Support", Icons.AutoMirrored.Filled.Help)
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Name Card
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF7ED)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👨‍🎓", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = container.tokenStore.userName ?: "Varun Joshi",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = container.tokenStore.userEmail ?: "varun.joshi@lpu.in",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Items
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.icon, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = item.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                            Text(text = "›", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out Button
            Button(
                onClick = {
                    container.authRepository.logout()
                    onLoggedOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFEA580C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sign Out", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
