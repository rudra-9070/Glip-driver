# Architecture Documentation — Glip Driver MVP

## 1. Architectural Overview

The **Glip Driver** application is architected following Google's official Android Architecture Guidelines and modern Clean Architecture principles:

- **Presentation Layer**: Jetpack Compose with Material Design 3, declarative UI, unidirectional data flow (UDF), and Compose Navigation.
- **Domain Layer**: Clean Kotlin data models (`DriverProfile`, `Vehicle`, `DriverRoute`, `PassengerRequest`, `DemoTrip`) and pure business logic use cases (`RouteMatchingEngine`, `InputValidationUtil`).
- **Data Layer**: Clean Repository pattern (`DriverRepository`), backing data through `DemoDriverRepositoryImpl`, `LocalDemoSessionManager` (using SharedPreferences), and seed datasets (`DemoSeedData`).

```
+--------------------------------------------------------------+
|                     Presentation Layer                       |
|   (Jetpack Compose M3 Screens, ViewModels / StateFlows, Nav)  |
+--------------------------------------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                        Domain Layer                          |
|    - RouteMatchingEngine (corridor matching & detour rules)  |
|    - InputValidationUtil (phone, OTP, capacity rules)        |
|    - DriverRepository Interface                              |
|    - Core Models (DriverProfile, Vehicle, DriverRoute...)    |
+--------------------------------------------------------------+
                               ^
                               |
+--------------------------------------------------------------+
|                         Data Layer                           |
|    - DemoDriverRepositoryImpl (in-memory StateFlow caches)   |
|    - LocalDemoSessionManager (persistent shared prefs)       |
|    - DemoSeedData (sample NCR corridor routes & passengers)  |
+--------------------------------------------------------------+
```

---

## 2. Route-Based Ride Pooling — Core Matching Algorithm

Traditional cab services assign arbitrary pickup points to ad-hoc on-demand drivers. Glip operates on **route-based corridor sharing**:
1. The driver defines a planned route: $Origin \rightarrow [Stop_1, Stop_2, ...] \rightarrow Destination$.
2. Commuter requests define: $Pickup \rightarrow Dropoff$ with requested seat count $S_{req}$.
3. **Capacity Gate**: If $S_{req} > S_{available}$, matching immediately marks the request as `NOT_COMPATIBLE` ("Seat capacity exceeded").
4. **Directional & Stop Sequence Check**:
   - If both pickup and drop-off match sequential corridor waypoints and $Index_{pickup} < Index_{dropoff}$, it is classified as `GOOD_MATCH` (0 to 3 min detour).
   - If $Index_{pickup} \ge Index_{dropoff}$, it is flagged as `NOT_COMPATIBLE` ("Opposite travel direction").
   - If only one endpoint touches the corridor, it is flagged as `PARTIAL_MATCH` (corridor branch overlap).

---

## 3. Separation of Concerns & Future Backend Pluggability

All screens depend strictly on `DriverRepository`. When transitioning from the local Demo MVP to a live Supabase backend:
- The UI layer requires zero refactoring.
- Create `SupabaseDriverRepositoryImpl : DriverRepository`.
- Replace the repository initialization in `MainActivity.kt`.
