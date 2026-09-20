# Trackly — Real-Time Intelligent Logistics & Package Delivery Ecosystem

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green.svg?logo=android)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Multi--Module-purple.svg)
![RealTime](https://img.shields.io/badge/RealTime-WebSockets-red.svg)
![AI](https://img.shields.io/badge/AI-Google%20Gemini-sparkles.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

> **Trackly** is a production-grade, multi-module Android application and real-time logistics tracking platform built from the ground up by **Yash Jaint**. It features live driver GPS location streaming over WebSockets, custom Google Maps rendering with 60fps marker interpolation, dynamic route ETA computation, an AI-powered assistant (Gemini API), and a Kotlin Ktor server backend.

---

## 👨‍💻 Author & Original Ownership Notice

**Trackly** was conceptualized, architected, and engineered by **Yash Jaint**.

- **Author**: Yash Jaint
- **GitHub**: [@yashjaint](https://github.com/yashjaint)
- **LinkedIn**: [Yash Jaint](https://www.linkedin.com/in/yash-jaint/)
- **Copyright**: © 2026 Yash Jaint. Licensed under the [MIT License](LICENSE).

---

## 📱 Android Client Features & Architecture

Trackly's Android client is engineered adhering to **SDE-2 Clean Architecture** standards and Android Jetpack guidelines.

### 🏛️ Multi-Module Architecture
The application is decoupled into 8+ specialized feature and core modules:
- `:app` — Entry point, Hilt dependency graph container, and global Navigation host.
- `:feature:customer-tracking` — Customer portal featuring active delivery status timeline, custom order creation with address search autocomplete, and live map view.
- `:feature:driver-delivery` — Driver portal for accepting delivery jobs, updating order milestones, and broadcasting live location.
- `:feature:ai-assistant` — Gemini-powered AI chatbot sheet for instant order status inquiries and delivery assistance.
- `:feature:auth` — Authentication screens (Login, Registration with role selection & vehicle number).
- `:core:network` — Domain repositories (`AuthRepository`, `OrderRepository`, `AddressSearchRepository`) and Data implementations (`Impl`) enforcing strict layer separation.
- `:core:location` — Foreground Service (`LocationService.kt`) broadcasting high-accuracy GPS coordinates in the background.
- `:core:websocket` — Ktor WebSockets engine (`TrackingWebSocketClient.kt`) managing full-duplex real-time location frame streams.
- `:core:common` — Custom Jetpack Compose UI design system tokens, `LiveTrackingMapView`, and Account Details modal.
- `:core:model` — Type-safe domain models and status enums.

---

## 🔥 Key Technical Highlights

1. **Full-Duplex WebSockets Real-Time Location Engine**:
   - Streams live driver GPS coordinates (`lat`, `lng`) to subscribed customer clients via `/ws/location`.
2. **Foreground Location Broadcast Service**:
   - Persistent Android `ForegroundService` with heads-up status bar notifications for uninterrupted background tracking.
3. **Google Maps SDK & Smooth 60fps Marker Interpolation**:
   - Custom map composable featuring dynamic pickup/dropoff markers, route polyline rendering, auto-adjusting camera bounds, and smooth vehicle marker position interpolation (`animateFloatAsState`).
4. **Distance & Dynamic Haversine Route ETA**:
   - Dynamic real-time arrival estimation computed using Haversine spherical distance calculations and speed metrics rather than hardcoded static fallbacks.
5. **Integrated Gemini AI Assistant**:
   - Interactive bottom-sheet AI assistant allowing customers to inquire about their active orders in natural language using the Google Gemini API.
6. **Cascading Account Deletion Rules**:
   - Conditional account deletion business logic preventing account deletion during active deliveries for both customers and drivers, backed by cascading database foreign key cleanup.

---

## 🛠️ Technology Stack

| Layer | Technologies Used |
| :--- | :--- |
| **Android UI** | Jetpack Compose, Material Design 3, Glassmorphism design tokens |
| **Android Core** | Kotlin, Coroutines, StateFlow, Flow |
| **Architecture** | Clean Architecture (Domain / Data / UI), Multi-Module |
| **Dependency Injection** | Dagger Hilt |
| **GIS & Maps** | Google Maps SDK for Android, FusedLocationProviderClient |
| **Networking** | Retrofit, OkHttp3, Ktor Client WebSockets |
| **AI Integration** | Google Gemini API (Firebase AI Logic / REST) |
| **Backend Server** | Ktor Server (Netty Engine), Exposed ORM, HikariCP |
| **Database** | Persistent H2 Database with PostgreSQL compatibility mode |
| **Security** | JWT Authentication, BCrypt Password Hashing |

---

## 📁 Project Directory Layout

```
Trackly/
├── android/                   # Multi-Module Jetpack Compose Android Client
│   ├── app/                   # App Navigation & Hilt Setup
│   ├── core/                  # Core Modules (Model, Network, Location, WebSocket, Common)
│   └── feature/               # Feature Modules (Auth, Customer, Driver, AI Assistant)
├── backend/                   # Asynchronous Ktor Kotlin Backend Monolith
│   └── src/main/kotlin/com/trackly/
│       ├── core/              # Database Factory & JWT Security
│       └── features/          # Auth, Order Management, Driver Location & WebSockets
├── LICENSE                    # MIT Copyright License (Yash Jain)
└── README.md                  # Project Documentation
```

---

## 🚀 Quick Start & Installation

### 1. Start the Backend Server
```bash
cd backend
export JAVA_HOME="/path/to/jdk-17"
./gradlew run
```
The backend server runs on `http://0.0.0.0:8080`.

### 2. Build & Deploy the Android App
Connect an Android device or emulator and run:
```bash
adb reverse tcp:8080 tcp:8080
cd android
./gradlew installDebug
```

---

## 📜 License & Copyright

Copyright © 2026 **Yash Jaint**.  
This project is released under the [MIT License](LICENSE).
