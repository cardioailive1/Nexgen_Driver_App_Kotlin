package com.corverxis.nexgendriver

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.corverxis.nexgendriver.ui.screens.*
import com.corverxis.nexgendriver.ui.theme.NexgenDriverTheme
import com.corverxis.nexgendriver.viewmodel.AppScreen
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import com.corverxis.nexgendriver.viewmodel.DriverViewModelFactory

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* LocationProvider.startUpdating() requires the permission to already be granted;
          DashboardScreen's "Waiting for GPS fix…" state naturally covers the case where
          the user denies it — they just can't go online until they grant it and reopen. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            val viewModel: DriverViewModel = viewModel(factory = DriverViewModelFactory(this))
            val screen by viewModel.screen.collectAsState()

            NexgenDriverTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        AppScreen.LOGIN -> LoginScreen(viewModel)
                        AppScreen.APPLICATION -> ApplicationScreen(viewModel)
                        AppScreen.DASHBOARD -> DashboardScreen(viewModel)
                        AppScreen.TRIP -> TripScreen(viewModel)
                        AppScreen.COMPLETE -> TripCompleteScreen(viewModel)
                    }
                }
            }
        }
    }
}
