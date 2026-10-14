package com.ashish.leadzam.data.model

import java.util.UUID

enum class RequestStatus {
    PENDING,
    DIALED,
    DISMISSED,
    INVALID
}

/**
 * Data model representing an incoming call request from the CRM backend.
 */
data class DialRequest(
    val id: String = UUID.randomUUID().toString(),
    val contactName: String? = null,
    val phoneNumber: String,
    val source: String = "CRM Request",
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING,
    val validationError: String? = null
) {
    val displayContactName: String
        get() = if (!contactName.isNullOrBlank()) contactName.trim() else "Unknown Contact"
}
