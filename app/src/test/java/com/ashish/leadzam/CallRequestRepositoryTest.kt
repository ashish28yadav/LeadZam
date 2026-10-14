package com.ashish.leadzam

import com.ashish.leadzam.data.model.RequestStatus
import com.ashish.leadzam.data.repository.CallRequestRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CallRequestRepositoryTest {

    @Before
    fun setUp() {
        CallRequestRepository.clearActiveRequest()
    }

    @Test
    fun `addIncomingRequest with valid payload creates pending active request`() {
        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = "+919876543210",
            contactName = "Rahul Sharma",
            source = "CRM Request"
        )

        assertEquals(RequestStatus.PENDING, request.status)
        assertEquals("+919876543210", request.phoneNumber)
        assertEquals("Rahul Sharma", request.displayContactName)

        val active = CallRequestRepository.activeRequest.value
        assertNotNull(active)
        assertEquals(request.id, active?.id)
    }

    @Test
    fun `addIncomingRequest with null contact name uses default display name`() {
        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = "9876543210",
            contactName = null
        )

        assertEquals("Unknown Contact", request.displayContactName)
    }

    @Test
    fun `addIncomingRequest with invalid phone number marks request invalid`() {
        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = "123",
            contactName = "Test User"
        )

        assertEquals(RequestStatus.INVALID, request.status)
        assertNotNull(request.validationError)
    }

    @Test
    fun `dismissRequest clears active request and updates status to DISMISSED`() {
        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = "+919876543210",
            contactName = "Rahul Sharma"
        )

        CallRequestRepository.dismissRequest(request.id)

        assertNull(CallRequestRepository.activeRequest.value)
        val historyItem = CallRequestRepository.getRequestById(request.id)
        assertEquals(RequestStatus.DISMISSED, historyItem?.status)
    }

    @Test
    fun `markRequestDialed clears active request and updates status to DIALED`() {
        val request = CallRequestRepository.addIncomingRequest(
            phoneNumber = "+919876543210",
            contactName = "Rahul Sharma"
        )

        CallRequestRepository.markRequestDialed(request.id)

        assertNull(CallRequestRepository.activeRequest.value)
        val historyItem = CallRequestRepository.getRequestById(request.id)
        assertEquals(RequestStatus.DIALED, historyItem?.status)
    }
}
