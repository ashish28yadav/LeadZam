package com.ashish.leadzam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashish.leadzam.ui.components.HistorySection
import com.ashish.leadzam.ui.components.IncomingCallCard
import com.ashish.leadzam.ui.components.StatusCard
import com.ashish.leadzam.ui.theme.LeadZamTheme
import com.ashish.leadzam.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val token by viewModel.fcmToken.collectAsState()
    val activeRequest by viewModel.activeRequest.collectAsState()
    val history by viewModel.requestHistory.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LeadZam",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automated FCM Click-to-Call Handler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Connection and notification status.
            StatusCard(
                isFcmReady = !token.isNullOrBlank(),
                isNotificationPermissionGranted = isPermissionGranted,
                onRequestPermission = onRequestNotificationPermission
            )

            // 2. Incoming request appears directly below the status card.
            activeRequest?.let { request ->
                IncomingCallCard(
                    request = request,
                    onOpenDialer = { req ->
                        viewModel.onOpenDialer(context, req)
                    },
                    onDismiss = { id ->
                        viewModel.onDismissRequest(id)
                    }
                )
            }

            // 3. Request history.
            HistorySection(
                history = history,
                onOpenDialer = { req ->
                    viewModel.onOpenDialer(context, req)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(
    name = "LeadZam Home Screen",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun LeadZamHomeScreenPreview() {
    LeadZamTheme {
        HomeScreen(
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            onRequestNotificationPermission = {}
        )
    }
}