package com.ashish.leadzam.data.validation

data class ValidationResult(
    val isValid: Boolean,
    val sanitizedNumber: String,
    val errorMessage: String? = null
)

object PhoneNumberValidator {

    /**
     * Validates and sanitizes phone numbers for the native dialer.
     * Rejects empty, too short (< 7 digits), too long (> 15 digits), or malformed inputs.
     */
    fun validate(rawPhoneNumber: String?): ValidationResult {
        if (rawPhoneNumber.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                sanitizedNumber = "",
                errorMessage = "Phone number is missing or empty."
            )
        }

        val trimmed = rawPhoneNumber.trim()

        // Sanitize: strip spaces, dashes, brackets, dots
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) {
            return ValidationResult(
                isValid = false,
                sanitizedNumber = trimmed,
                errorMessage = "Phone number contains no valid digits."
            )
        }

        // Standard phone number digit length check (7 to 15 digits per E.164 standards)
        if (digitsOnly.length < 7) {
            return ValidationResult(
                isValid = false,
                sanitizedNumber = trimmed,
                errorMessage = "Phone number is too short (${digitsOnly.length} digits, minimum 7)."
            )
        }

        if (digitsOnly.length > 15) {
            return ValidationResult(
                isValid = false,
                sanitizedNumber = trimmed,
                errorMessage = "Phone number exceeds maximum length of 15 digits."
            )
        }

        // Reconstruct sanitized number
        val sanitizedNumber = if (hasLeadingPlus) "+$digitsOnly" else digitsOnly

        return ValidationResult(
            isValid = true,
            sanitizedNumber = sanitizedNumber
        )
    }
}
