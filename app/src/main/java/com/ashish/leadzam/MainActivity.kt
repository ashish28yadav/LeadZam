package com.ashish.leadzam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.ashish.leadzam.data.model.RequestStatus
import com.ashish.leadzam.data.repository.CallRequestRepository
import com.ashish.leadzam.messaging.NotificationHelper
import com.ashish.leadzam.ui.screens.HomeScreen
import com.ashish.leadzam.ui.theme.LeadZamTheme
import com.ashish.leadzam.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.setNotificationPermissionGranted(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()
        handleNotificationIntent(intent)

        setContent {
            LeadZamTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onRequestNotificationPermission = { requestNotificationPermission() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.setNotificationPermissionGranted(granted)
        } else {
            viewModel.setNotificationPermissionGranted(true)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val requestId = intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID)
        val phoneNumber = intent.getStringExtra(NotificationHelper.EXTRA_PHONE_NUMBER)
        val contactName = intent.getStringExtra(NotificationHelper.EXTRA_CONTACT_NAME)
        val source = intent.getStringExtra(NotificationHelper.EXTRA_SOURCE)
        val autoDial = intent.getBooleanExtra(NotificationHelper.EXTRA_AUTO_DIAL, false)

        if (!phoneNumber.isNullOrBlank()) {
            val request = CallRequestRepository.addIncomingRequest(
                phoneNumber = phoneNumber,
                contactName = contactName,
                source = source ?: "CRM Notification",
                requestId = requestId
            )

            if (autoDial && request.status == RequestStatus.PENDING) {
                viewModel.onOpenDialer(this, request)
            }
        }
    }
}
