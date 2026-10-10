package com.glip.driver.domain.usecase

object InputValidationUtil {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    fun validateMobileNumber(mobile: String): ValidationResult {
        val clean = mobile.filter { it.isDigit() }
        if (clean.isBlank()) {
            return ValidationResult(false, "Mobile number is required.")
        }
        if (clean.length != 10) {
            return ValidationResult(false, "Please enter a valid 10-digit mobile number.")
        }
        if (clean.first() !in '6'..'9') {
            return ValidationResult(false, "Indian mobile numbers must start with 6, 7, 8, or 9.")
        }
        return ValidationResult(true)
    }

    fun validateOtp(otp: String): ValidationResult {
        val clean = otp.filter { it.isDigit() }
        if (clean.length != 6) {
            return ValidationResult(false, "OTP must be exactly 6 digits.")
        }
        return ValidationResult(true)
    }

    fun validateFullName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            return ValidationResult(false, "Please enter a valid full name (at least 2 letters).")
        }
        return ValidationResult(true)
    }

    fun validateVehicleSeats(capacity: Int, occupied: Int): ValidationResult {
        if (capacity <= 0) {
            return ValidationResult(false, "Vehicle capacity must be greater than 0.")
        }
        if (occupied < 0) {
            return ValidationResult(false, "Occupied seats cannot be negative.")
        }
        if (occupied > capacity) {
            return ValidationResult(false, "Occupied seats ($occupied) cannot exceed total capacity ($capacity).")
        }
        return ValidationResult(true)
    }

    fun validateRoute(origin: String, destination: String): ValidationResult {
        if (origin.trim().length < 2) {
            return ValidationResult(false, "Starting point is required.")
        }
        if (destination.trim().length < 2) {
            return ValidationResult(false, "Destination is required.")
        }
        if (origin.trim().equals(destination.trim(), ignoreCase = true)) {
            return ValidationResult(false, "Starting point and destination cannot be identical.")
        }
        return ValidationResult(true)
    }
}
