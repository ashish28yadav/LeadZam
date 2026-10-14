package com.ashish.leadzam.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashish.leadzam.data.model.DialRequest
import com.ashish.leadzam.data.repository.CallRequestRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    val fcmToken: StateFlow<String?> = CallRequestRepository.fcmToken
    val tokenError: StateFlow<String?> = CallRequestRepository.tokenError
    val activeRequest: StateFlow<DialRequest?> = CallRequestRepository.activeRequest
    val requestHistory: StateFlow<List<DialRequest>> = CallRequestRepository.requestHistory
    val userMessage: StateFlow<String?> = CallRequestRepository.userMessage

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isFetchingToken = MutableStateFlow(false)
    val isFetchingToken: StateFlow<Boolean> = _isFetchingToken.asStateFlow()

    init {
        fetchFcmToken()
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
    }

    fun fetchFcmToken() {
        _isFetchingToken.value = true
        CallRequestRepository.setTokenError(null)
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    _isFetchingToken.value = false
                    if (!task.isSuccessful) {
                        val exception = task.exception
                        val errorDetail = exception?.localizedMessage ?: exception?.message ?: "Unknown error"
                        Log.e("HomeViewModel", "Fetching FCM registration token failed", exception)

                        val displayError = if (errorDetail.contains("FIS_AUTH_ERROR") || errorDetail.contains("API key")) {
                            "Firebase authentication failed (FIS_AUTH_ERROR). Please download a valid google-services.json from your Firebase Console and place it in the app/ folder."
                        } else {
                            "Failed to retrieve FCM token: $errorDetail"
                        }

                        CallRequestRepository.setTokenError(displayError)
                        CallRequestRepository.setUserMessage(displayError)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        Log.d("HomeViewModel", "FCM token fetched successfully: $token")
                        CallRequestRepository.updateFcmToken(token)
                    } else {
                        val error = "Received empty FCM token from Firebase."
                        Log.w("HomeViewModel", error)
                        CallRequestRepository.setTokenError(error)
                    }
                }
        } catch (e: Exception) {
            _isFetchingToken.value = false
            val errorMsg = "Error initializing FCM token retrieval: ${e.localizedMessage ?: e.message}"
            Log.e("HomeViewModel", errorMsg, e)
            CallRequestRepository.setTokenError(errorMsg)
            CallRequestRepository.setUserMessage(errorMsg)
        }
    }

    fun copyTokenToClipboard(context: Context) {
        val token = fcmToken.value
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "No FCM token available to copy.", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("FCM Registration Token", token)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun onOpenDialer(context: Context, request: DialRequest) {
        try {
            val phoneUri = Uri.fromParts("tel", request.phoneNumber, null)
            val intent = Intent(Intent.ACTION_DIAL, phoneUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CallRequestRepository.markRequestDialed(request.id)
            CallRequestRepository.setUserMessage("Dialer opened for ${request.displayContactName}")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to open native dialer", e)
            CallRequestRepository.setUserMessage("Unable to open dialer: ${e.localizedMessage}")
        }
    }

    fun onDismissRequest(requestId: String) {
        CallRequestRepository.dismissRequest(requestId)
    }

    fun clearUserMessage() {
        CallRequestRepository.clearUserMessage()
    }

    fun simulateTestRequest(phoneNumber: String = "+919876543210", contactName: String = "Rahul Sharma") {
        viewModelScope.launch {
            CallRequestRepository.addIncomingRequest(
                phoneNumber = phoneNumber,
                contactName = contactName,
                source = "Demo / Manual Test"
            )
            CallRequestRepository.setUserMessage("Simulated test request added!")
        }
    }
}
