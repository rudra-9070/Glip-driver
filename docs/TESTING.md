# Testing Strategy & Test Suite — Glip Driver

## 1. Test Suite Summary

The unit test suite covers all critical business rules in `app/src/test/java/com/glip/driver/DriverUnitTests.kt`:

1. **Input Validation Tests**:
   - `testMobileNumberValidation`: Validates Indian 10-digit mobile standards (starting with 6, 7, 8, or 9), rejecting invalid lengths and alphabetic characters.
   - `testOtpValidation`: Verifies 6-digit numeric constraints.
   - `testVehicleCapacityValidation`: Enforces positive capacity ($>0$), prevents negative occupied seats, and rejects over-capacity configurations ($Occupied > Capacity$).
   - `testRouteValidation`: Validates non-empty origins and destinations and rejects identical endpoints.

2. **Route-Based Matching Engine Tests**:
   - `testRouteMatching_GoodSequentialMatch`: Confirms that passengers travelling in the forward sequence along the driver's planned corridor receive a `GOOD_MATCH` rating.
   - `testRouteMatching_CapacityExceeded_Rejected`: Asserts that passenger requests exceeding the driver's available seats are rejected (`NOT_COMPATIBLE`) with an informative capacity explanation.
   - `testRouteMatching_ReverseDirection_Rejected`: Verifies that requests in the opposite travel direction are marked as `NOT_COMPATIBLE`.
   - `testRouteMatching_PartialMatch`: Validates branch corridor matches.

---

## 2. Manual QA / Verification Checklist

| Journey | Expected Outcome | Status |
|---|---|---|
| 1. Fresh Launch | App opens `WelcomeScreen` with demo banner | Verified |
| 2. Registration | 10-digit mobile input generates 6-digit demo OTP | Verified |
| 3. OTP Limit | 3 incorrect attempts locks the prompt and requires requesting a new OTP | Verified |
| 4. Vehicle Setup | Stepper updates capacity and calculates available seats ($C - O$) | Verified |
| 5. Route Setup | Origin, destination, corridor direction & waypoints save | Verified |
| 6. Home Dashboard | Live route corridor preview, availability toggle, capacity metrics | Verified |
| 7. Request Matching | Incompatible or over-capacity requests cannot be accepted | Verified |
| 8. Request Accept | Prompts modal confirmation, reduces available seats, logs to Activity | Verified |
| 9. Activity Tab | Displays Accepted, Declined, Completed and Cancelled records | Verified |
| 10. Data Clear | "Reset & Clear All Demo Data" flushes state and returns to Welcome | Verified |
