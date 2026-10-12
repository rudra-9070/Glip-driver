package com.glip.driver

import com.glip.driver.data.demo.DemoSeedData
import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.domain.usecase.RouteMatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverUnitTests {

    private lateinit var matchingEngine: RouteMatchingEngine

    @Before
    fun setUp() {
        matchingEngine = RouteMatchingEngine()
    }

    @Test
    fun testMobileNumberValidation() {
        // Valid 10-digit starting with 9
        val valid = InputValidationUtil.validateMobileNumber("9876543210")
        assertTrue("Mobile number starting with 9 should be valid", valid.isValid)

        // Invalid length
        val shortNumber = InputValidationUtil.validateMobileNumber("98765")
        assertFalse("Short mobile number should be rejected", shortNumber.isValid)

        // Invalid prefix
        val invalidPrefix = InputValidationUtil.validateMobileNumber("1234567890")
        assertFalse("Mobile starting with 1 should be rejected", invalidPrefix.isValid)

        // Empty
        val empty = InputValidationUtil.validateMobileNumber("")
        assertFalse("Empty mobile number should be rejected", empty.isValid)
    }

    @Test
    fun testOtpValidation() {
        assertTrue(InputValidationUtil.validateOtp("123456").isValid)
        assertFalse(InputValidationUtil.validateOtp("12345").isValid)
        assertFalse(InputValidationUtil.validateOtp("1234567").isValid)
        assertFalse(InputValidationUtil.validateOtp("abcdef").isValid)
    }

    @Test
    fun testVehicleCapacityValidation() {
        // Normal valid setup
        val valid = InputValidationUtil.validateVehicleSeats(capacity = 4, occupied = 2)
        assertTrue(valid.isValid)

        // Zero capacity is invalid
        val zeroCapacity = InputValidationUtil.validateVehicleSeats(capacity = 0, occupied = 0)
        assertFalse(zeroCapacity.isValid)

        // Occupied exceeds capacity
        val overloaded = InputValidationUtil.validateVehicleSeats(capacity = 3, occupied = 4)
        assertFalse(overloaded.isValid)

        // Negative occupied
        val negativeOccupied = InputValidationUtil.validateVehicleSeats(capacity = 3, occupied = -1)
        assertFalse(negativeOccupied.isValid)
    }

    @Test
    fun testRouteMatching_GoodSequentialMatch() {
        val route = DemoSeedData.defaultRoute()
        val vehicle = DemoSeedData.defaultVehicle() // capacity=3, occupied=1 => available=2

        // Request Pragati Maidan -> Mayur Vihar Phase 1 (both along the route sequentially)
        val result = matchingEngine.evaluateMatch(
            driverRoute = route,
            vehicle = vehicle,
            pickupName = "Pragati Maidan",
            dropoffName = "Mayur Vihar Phase 1",
            requestedSeats = 1
        )

        assertEquals(CompatibilityLevel.GOOD_MATCH, result.level)
        assertTrue(result.isCapacityAvailable)
        assertNotNull(result.estimatedDetourMinutes)
    }

    @Test
    fun testRouteMatching_CapacityExceeded_Rejected() {
        val route = DemoSeedData.defaultRoute()
        val vehicle = DemoSeedData.defaultVehicle() // availableSeats = 2

        // Request asking for 4 seats exceeds 2 available seats
        val result = matchingEngine.evaluateMatch(
            driverRoute = route,
            vehicle = vehicle,
            pickupName = "Pragati Maidan",
            dropoffName = "Mayur Vihar Phase 1",
            requestedSeats = 4
        )

        assertEquals(CompatibilityLevel.NOT_COMPATIBLE, result.level)
        assertFalse(result.isCapacityAvailable)
        assertTrue(result.explanation.contains("available seat"))
    }

    @Test
    fun testRouteMatching_ReverseDirection_Rejected() {
        val route = DemoSeedData.defaultRoute() // Connaught Place -> Pragati Maidan -> Akshardham -> Mayur Vihar -> Noida
        val vehicle = DemoSeedData.defaultVehicle()

        // Reverse direction: Mayur Vihar -> Connaught Place
        val result = matchingEngine.evaluateMatch(
            driverRoute = route,
            vehicle = vehicle,
            pickupName = "Mayur Vihar Phase 1",
            dropoffName = "Connaught Place (Inner Circle)",
            requestedSeats = 1
        )

        assertEquals(CompatibilityLevel.NOT_COMPATIBLE, result.level)
        assertTrue(result.explanation.contains("Opposite travel direction"))
    }

    @Test
    fun testRouteMatching_PartialMatch() {
        val route = DemoSeedData.defaultRoute()
        val vehicle = DemoSeedData.defaultVehicle()

        // Pickup is along the route (Akshardham), but destination is an unknown local branch
        val result = matchingEngine.evaluateMatch(
            driverRoute = route,
            vehicle = vehicle,
            pickupName = "Akshardham Flyover",
            dropoffName = "Sector 62 IT Park",
            requestedSeats = 1
        )

        assertEquals(CompatibilityLevel.PARTIAL_MATCH, result.level)
        assertTrue(result.explanation.contains("Corridor overlap"))
    }

    @Test
    fun testRouteValidation() {
        assertTrue(InputValidationUtil.validateRoute("Connaught Place", "Noida").isValid)
        assertFalse(InputValidationUtil.validateRoute("A", "Noida").isValid)
        assertFalse(InputValidationUtil.validateRoute("Noida", "Noida").isValid)
    }

    @Test
    fun testCrashReportCreation() {
        val testEx = IllegalArgumentException("Test exception message")
        val report = com.glip.driver.util.CrashManager.createReport(testEx)
        assertEquals("IllegalArgumentException", report.errorType)
        assertEquals("Test exception message", report.errorMessage)
        assertTrue(report.stackTrace.contains("IllegalArgumentException"))
        assertTrue(report.deviceInfo.isNotEmpty())
    }
}
