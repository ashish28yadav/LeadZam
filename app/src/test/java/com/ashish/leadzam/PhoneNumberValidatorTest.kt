package com.ashish.leadzam

import com.ashish.leadzam.data.validation.PhoneNumberValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberValidatorTest {

    @Test
    fun `validate standard E164 international phone number returns valid`() {
        val result = PhoneNumberValidator.validate("+919876543210")
        assertTrue(result.isValid)
        assertEquals("+919876543210", result.sanitizedNumber)
    }

    @Test
    fun `validate local formatted number sanitizes non-digits correctly`() {
        val result = PhoneNumberValidator.validate("+1 (555) 019-2834")
        assertTrue(result.isValid)
        assertEquals("+15550192834", result.sanitizedNumber)
    }

    @Test
    fun `validate number without plus returns digits`() {
        val result = PhoneNumberValidator.validate("9876543210")
        assertTrue(result.isValid)
        assertEquals("9876543210", result.sanitizedNumber)
    }

    @Test
    fun `validate null or blank phone number returns invalid`() {
        val nullResult = PhoneNumberValidator.validate(null)
        assertFalse(nullResult.isValid)

        val emptyResult = PhoneNumberValidator.validate("   ")
        assertFalse(emptyResult.isValid)
    }

    @Test
    fun `validate too short phone number returns invalid`() {
        val result = PhoneNumberValidator.validate("12345")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("too short") == true)
    }

    @Test
    fun `validate non-numeric string returns invalid`() {
        val result = PhoneNumberValidator.validate("invalid-number")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("no valid digits") == true)
    }

    @Test
    fun `validate too long phone number returns invalid`() {
        val longNum = "123456789012345678"
        val result = PhoneNumberValidator.validate(longNum)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("maximum length") == true)
    }
}
