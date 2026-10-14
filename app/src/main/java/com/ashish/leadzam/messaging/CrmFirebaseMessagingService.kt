package com.ashish.leadzam.messaging

import android.util.Log
import com.ashish.leadzam.data.model.RequestStatus
import com.ashish.leadzam.data.repository.CallRequestRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CrmFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
        CallRequestRepository.updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received from: ${remoteMessage.from}")

        val dataMap: Map<String, String> = remoteMessage.data
        if (dataMap.isEmpty()) {
            Log.w(TAG, "Received FCM message with empty data payload.")
            return
        }

        val type: String? = dataMap["type"]
        if (type != TYPE_DIAL_REQUEST) {
            Log.w(TAG, "Ignoring unsupported FCM message type: $type")
            return
        }

        val phoneNumber: String? = dataMap["phoneNumber"]
        val contactName: String? = dataMap["contactName"]
        val source: String = dataMap["source"] ?: "CRM Request"
        val requestId: String? = dataMap["requestId"] ?: dataMap["id"]

        Log.d(TAG, "Processing dial_request for number: $phoneNumber, contact: $contactName")

        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = phoneNumber,
            contactName = contactName,
            source = source,
            requestId = requestId
        )

        // Show notification if valid pending request
        if (request.status == RequestStatus.PENDING) {
            NotificationHelper.showCallNotification(applicationContext, request)
        }
    }

    companion object {
        private const val TAG = "CrmFCMService"
        private const val TYPE_DIAL_REQUEST = "dial_request"
    }
}
