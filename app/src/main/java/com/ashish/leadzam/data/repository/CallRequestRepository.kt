package com.ashish.leadzam.data.repository

import com.ashish.leadzam.data.model.DialRequest
import com.ashish.leadzam.data.model.RequestStatus
import com.ashish.leadzam.data.validation.PhoneNumberValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Shared in-memory repository managing FCM registration tokens, incoming call requests,
 * and request execution history.
 */
object CallRequestRepository {

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _tokenError = MutableStateFlow<String?>(null)
    val tokenError: StateFlow<String?> = _tokenError.asStateFlow()

    private val _backendRegistrationStatus = MutableStateFlow(com.ashish.leadzam.data.api.BackendRegistrationStatus.NOT_REGISTERED)
    val backendRegistrationStatus: StateFlow<com.ashish.leadzam.data.api.BackendRegistrationStatus> = _backendRegistrationStatus.asStateFlow()

    private val _backendRegistrationError = MutableStateFlow<String?>(null)
    val backendRegistrationError: StateFlow<String?> = _backendRegistrationError.asStateFlow()

    private val _activeRequest = MutableStateFlow<DialRequest?>(null)
    val activeRequest: StateFlow<DialRequest?> = _activeRequest.asStateFlow()

    private val _requestHistory = MutableStateFlow<List<DialRequest>>(emptyList())
    val requestHistory: StateFlow<List<DialRequest>> = _requestHistory.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun updateFcmToken(token: String) {
        _fcmToken.value = token
        _tokenError.value = null
    }

    fun setTokenError(error: String?) {
        _tokenError.value = error
    }

    fun updateBackendRegistrationState(status: com.ashish.leadzam.data.api.BackendRegistrationStatus, error: String? = null) {
        _backendRegistrationStatus.value = status
        _backendRegistrationError.value = error
    }

    /**
     * Processes an incoming FCM call request, validates the phone number,
     * updates the active request, and records it in history.
     */
    fun addIncomingRequest(
        phoneNumber: String?,
        contactName: String?,
        source: String? = "CRM Request",
        requestId: String? = null
    ): DialRequest {
        val validationResult = PhoneNumberValidator.validate(phoneNumber)

        val id = if (!requestId.isNullOrBlank()) requestId else UUID.randomUUID().toString()
        val requestSource = if (!source.isNullOrBlank()) source else "CRM Request"

        val request = if (validationResult.isValid) {
            DialRequest(
                id = id,
                contactName = contactName,
                phoneNumber = validationResult.sanitizedNumber,
                source = requestSource,
                timestamp = System.currentTimeMillis(),
                status = RequestStatus.PENDING
            )
        } else {
            DialRequest(
                id = id,
                contactName = contactName,
                phoneNumber = phoneNumber ?: "",
                source = requestSource,
                timestamp = System.currentTimeMillis(),
                status = RequestStatus.INVALID,
                validationError = validationResult.errorMessage ?: "Invalid phone number format."
            )
        }

        if (request.status == RequestStatus.PENDING) {
            _activeRequest.value = request
        } else {
            _userMessage.value = "Incoming request contains invalid phone number: ${request.phoneNumber}"
        }

        _requestHistory.update { history ->
            // Replace existing request with same ID if present, otherwise prepend
            val filtered = history.filterNot { it.id == request.id }
            listOf(request) + filtered
        }

        return request
    }

    fun dismissRequest(requestId: String) {
        if (_activeRequest.value?.id == requestId) {
            _activeRequest.value = null
        }
        updateRequestStatus(requestId, RequestStatus.DISMISSED)
    }

    fun markRequestDialed(requestId: String) {
        if (_activeRequest.value?.id == requestId) {
            _activeRequest.value = null
        }
        updateRequestStatus(requestId, RequestStatus.DIALED)
    }

    fun getRequestById(requestId: String): DialRequest? {
        return _requestHistory.value.find { it.id == requestId }
    }

    fun clearActiveRequest() {
        _activeRequest.value = null
    }

    fun setUserMessage(message: String?) {
        _userMessage.value = message
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun updateRequestStatus(requestId: String, newStatus: RequestStatus) {
        _requestHistory.update { history ->
            history.map { req ->
                if (req.id == requestId) req.copy(status = newStatus) else req
            }
        }
    }
}
