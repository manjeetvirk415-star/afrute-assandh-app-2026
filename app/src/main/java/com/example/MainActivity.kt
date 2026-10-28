package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.model.AppRole
import com.example.ui.AfruteViewModel
import com.example.ui.components.TopHeader
import com.example.ui.customer.CustomerStorefrontScreen
import com.example.ui.owner.OwnerDashboardScreen
import com.example.ui.shopkeeper.ShopkeeperScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnyxBlack

class MainActivity : ComponentActivity() {

    private val viewModel: AfruteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AfruteApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AfruteApp(viewModel: AfruteViewModel) {
    val currentRole by viewModel.currentRole.collectAsState()
    val showTrackingScreen by viewModel.showTrackingScreen.collectAsState()

    // Back handling for secondary states
    BackHandler(enabled = showTrackingScreen) {
        viewModel.hideOrderTracking()
    }

    BackHandler(enabled = !showTrackingScreen && currentRole != AppRole.OWNER) {
        viewModel.switchRole(AppRole.OWNER)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .testTag("afrute_main_scaffold"),
        topBar = {
            TopHeader(
                currentRole = currentRole,
                onRoleSelected = { role -> viewModel.switchRole(role) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OnyxBlack)
        ) {
            when (currentRole) {
                AppRole.OWNER -> {
                    OwnerDashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppRole.SHOPKEEPER -> {
                    ShopkeeperScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppRole.CUSTOMER -> {
                    CustomerStorefrontScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
