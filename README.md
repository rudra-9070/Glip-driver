# Glip Driver — Android MVP

**Glip Driver** is the driver-side companion Android application to the passenger-side Glip platform. It is engineered specifically for **route-based ride pooling** across local city corridors (such as auto-rickshaws, e-rickshaws, cabs, and shared shuttles), allowing drivers who are already travelling along a planned route to monetize unoccupied seats with compatible co-passengers.

---

## 🚀 Key Features

1. **Brand Identity & Adaptive Icon**:
   - Custom steering wheel & navigation waypoint adaptive launcher icon.
   - Professional dark navy and cyan/green UI theme optimized for driver high-contrast readability.
2. **Demo Authentication & Onboarding**:
   - Simulated 6-digit OTP verification with 3-attempt limit and resend controls.
   - Dedicated driver profile, vehicle category configuration, and capacity/occupied seats counter.
3. **Planned Corridor Route Setup**:
   - Driver defines origin, destination, intermediate stops, approximate departure time, and direction.
4. **Driver Dashboard**:
   - One-tap availability toggle (Available for Route Sharing vs. Offline).
   - Seat capacity metrics (Total Capacity, Occupied, Available).
   - Interactive Route Corridor Map Preview showing sequential stops and projected passenger pickups.
5. **Route-Based Matching Engine**:
   - Pure domain usecase (`RouteMatchingEngine`) that filters requests based on corridor sequence and remaining seat capacity.
   - Categorizes requests as **Good Demo Match**, **Partial Route Match**, or **Not Compatible** with transparent explanations.
6. **Activity & Trip Management**:
   - Audit trail of accepted, declined, completed, and cancelled demo rides.
   - Ability to complete or cancel demo rides, which dynamically restores vehicle seat availability.
7. **Future Services Roadmap**:
   - Modular "Coming Soon" screens for **Parcel Delivery** and **School Transportation**.
8. **Privacy & Security Safeguards**:
   - Zero intrusive permissions (no SMS reading, no background tracking, no contacts, no camera).
   - Complete local data reset option in the Profile tab.

---

## 🛠 Tech Stack

- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture + MVVM + Repository Pattern
- **Navigation**: Jetpack Navigation Compose
- **Concurrency**: Kotlin Coroutines & StateFlow
- **Target SDK**: Android 34 (UpsideDownCake), Min SDK: Android 24 (Nougat)

---

## 📱 How to Build and Run on a Physical Phone

You can build and install this APK directly on your physical Android phone without needing an emulator or Android Studio open.

### Prerequisites
1. **Java JDK 17+** (Installed at `C:\Program Files\Common Files\Oracle\Java\javapath` or standard JDK).
2. **Android SDK & Build-Tools** (Command-line tools or Android Studio SDK location).
3. **USB Cable & Physical Phone**.

### Step 1: Enable USB Debugging on Your Phone
1. Go to **Settings** > **About Phone**.
2. Tap **Build Number** 7 times until you see "You are now a developer!".
3. Go back to **Settings** > **System / Additional Settings** > **Developer Options**.
4. Enable **USB Debugging** (and "Install via USB" if on Xiaomi / MIUI / Realme / Oppo devices).
5. Plug your phone into your computer via USB. On your phone, tap **Allow USB Debugging** on the prompt.

### Step 2: Build the APK
Open PowerShell in `C:\glipgo_driver` and run:
```powershell
# If using gradle wrapper:
.\gradlew assembleDebug

# Or if you have Gradle installed globally:
gradle assembleDebug
```
The compiled debug APK will be generated at:
`app\build\outputs\apk\debug\app-debug.apk`

### Step 3: Install Directly to Phone via ADB
Run:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
*Alternatively, you can simply copy the generated `app-debug.apk` file to your phone's storage via USB cable or WhatsApp/Telegram/Drive, tap it on your phone, and select **Install**.*

---

## 🧪 Running Unit Tests

To run the business logic, validation, and route-matching unit tests:
```powershell
.\gradlew testDebugUnitTest
```

---

## 📂 Project Structure

```
c:/glipgo_driver/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/glip/driver/
│       │   │   ├── MainActivity.kt
│       │   │   ├── domain/model/          # DriverProfile, Vehicle, DriverRoute, PassengerRequest, DemoTrip...
│       │   │   ├── domain/usecase/        # RouteMatchingEngine, InputValidationUtil
│       │   │   ├── domain/repository/     # DriverRepository interface
│       │   │   ├── data/local/            # LocalDemoSessionManager (SharedPreferences)
│       │   │   ├── data/demo/             # DemoSeedData (NCR sample corridors & requests)
│       │   │   ├── data/repository/       # DemoDriverRepositoryImpl
│       │   │   ├── map/                   # RouteCorridorMapPreview & RouteStopFallbackList
│       │   │   ├── navigation/            # GlipDriverNavHost, Screen, BottomNavItem
│       │   │   └── ui/                    # Theme, Components, Auth, Onboarding, Home, Requests, Activity, Profile...
│       │   └── res/                       # Adaptive icons, colors, strings, themes
│       └── test/                          # DriverUnitTests.kt
├── docs/                                  # ARCHITECTURE.md, SECURITY.md, TESTING.md, FUTURE_BACKEND.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```
