package com.example.warehousescan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.warehousescan.core.storage.SecureTokenStore
import com.example.warehousescan.feature.admin.AdminScreen
import com.example.warehousescan.feature.dispatchverification.DispatchVerificationScreen
import com.example.warehousescan.feature.home.HomeScreen
import com.example.warehousescan.feature.locationsetting.LocationSettingScreen
import com.example.warehousescan.feature.login.LoginScreen
import com.example.warehousescan.feature.register.RegisterScreen
import com.example.warehousescan.feature.usermanagement.UserManagementScreen
import com.example.warehousescan.ui.theme.WarehousescanTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStore: SecureTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WarehousescanTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    WarehouseNavHost(tokenStore = tokenStore)

                    if (BuildConfig.FLAVOR == "demo") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.TopCenter),
                            color = Color.Red
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("DEMO MODE", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseNavHost(tokenStore: SecureTokenStore) {
    val navController = rememberNavController()
    val startDestination = if (tokenStore.getToken().isNullOrBlank()) "login" else "home"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onOpenAdmin = {
                    navController.navigate("admin")
                },
                onNavigateRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateLocationSetting = {
                    navController.navigate("location_setting")
                },
                onNavigateDispatchVerification = {
                    navController.navigate("dispatch_verification")
                },
                onNavigateUserManagement = {
                    navController.navigate("user_management")
                },
                onLogout = {
                    tokenStore.clearToken()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("location_setting") {
            LocationSettingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("dispatch_verification") {
            DispatchVerificationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("admin") {
            AdminScreen(
                onBack = { navController.popBackStack() },
                onNavigateUserManagement = {
                    navController.navigate("user_management")
                }
            )
        }
        composable("user_management") {
            UserManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
