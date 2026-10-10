# Security & Privacy Guidelines — Glip Driver

## 1. Minimal Permissions Architecture

The Glip Driver MVP strictly adheres to the principle of least privilege:
- **No Background Location Permission**: The driver is never tracked in the background. Simulated coordinates and corridors are used for demo purposes.
- **No SMS Reading Permissions**: We do NOT request `READ_SMS` or `RECEIVE_SMS`. Demo verification uses an explicit on-screen sandbox flow.
- **No Contacts or Camera Permissions**: Zero invasive permissions are requested in `AndroidManifest.xml`.
- Only `android.permission.INTERNET` is declared for network mapping tiles and future backend queries.

---

## 2. Authentication & Verification Safeguards

1. **Explicit Demo Labelling**: The app explicitly informs users that the OTP generation is a local simulator. No claim is made that an external SMS was dispatched.
2. **Attempt Limiting**: OTP verification is capped at 3 attempts per session to model brute-force resistance.
3. **Debug vs Release Configuration**: The demo OTP code is displayed via `BuildConfig.SHOW_DEMO_OTP` which is explicitly set to `false` in release builds.
4. **No Sensitive PII Plaintext Logging**: Phone numbers and OTP values are not outputted to production Logcat streams.

---

## 3. Data Protection & Privacy

- **Local Storage**: Minimum profile information (name, masked phone, vehicle preferences) is stored via Android `SharedPreferences`.
- **Wipe Mechanism**: Drivers can completely wipe all local profile data and reset vehicle state via the "Reset & Clear All Demo Data" button in the Profile tab.
- **Driver Distraction Safeguards**: The app prominently displays road safety warnings reminding drivers never to interact with the device while operating a moving vehicle.

---

## 4. Indian Mobility & Regulatory Context

Before deploying a production pooling platform in India:
- Review the **Motor Vehicles Act (1988)**, the **Motor Vehicle Aggregator Guidelines (2020)**, and state-specific carpooling / stage carriage policies.
- Ensure verified commercial driving licenses (badge for transport vehicles where required), fitness certificates, and passenger liability insurance before real passengers are boarded.
- Do not claim regulatory endorsement or government approval in marketing materials without formal state transport permits.
