# Trackly — Real-Time Intelligent Delivery System

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green.svg?logo=android)
![Backend](https://img.shields.io/badge/Backend-Ktor%20Framework-orange.svg?logo=ktor)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![WebSockets](https://img.shields.io/badge/RealTime-WebSockets-red.svg)

Trackly is a production-grade, full-stack real-time delivery tracking platform featuring a **Jetpack Compose Android Application** and a high-performance **Ktor Kotlin Backend Monolith**.

Built with SDE-2 Clean Architecture standards, Trackly manages full delivery lifecycles, real-time GPS streaming over WebSockets, smooth 60fps map marker interpolation, and persistent state management.

---

## 🚀 Key Technical Highlights

- **Full-Duplex WebSockets Engine**: Live position broadcasting endpoint (`/ws/tracking/{orderId}`) streaming driver GPS updates to subscribed tracking clients in real-time.
- **Android Location Foreground Service**: Persistent, high-accuracy GPS tracking service (`LocationService.kt`) with heads-up status bar notifications and Android 14+ compatibility.
- **Smooth Google Maps Integration**: Custom `LiveTrackingMapView` featuring pickup/dropoff markers, route polylines, and **60fps vehicle marker interpolation** (`animateFloatAsState`) eliminating map marker jitter.
- **Order State Machine**: Strict pipeline enforcing deterministic status transitions (`CREATED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY_FOR_PICKUP` $\rightarrow$ `PICKED_UP` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`).
- **Security & Persistence**: JWT authentication, BCrypt password hashing, persistent H2 file database (`./build/trackly_db`), and encrypted client session storage.

---

## 🛠️ Technology Stack

### Android Client (`/android`)
- **UI Framework**: Jetpack Compose, Material 3, Custom Design System (Deep Ocean palette)
- **Architecture**: Multi-module Clean Architecture (`:app`, `:core:model`, `:core:network`, `:core:location`, `:core:websocket`, `:feature:auth`, `:feature:customer-tracking`, `:feature:driver-delivery`)
- **Dependency Injection**: Dagger Hilt
- **Async & Reactive**: Kotlin Coroutines, StateFlow, Flow
- **Maps & Location**: Google Maps Compose SDK, FusedLocationProviderClient

### Ktor Backend (`/backend`)
- **Server Framework**: Ktor 2.3 (Netty Engine)
- **Real-Time Streaming**: Ktor Server WebSockets
- **Database & ORM**: Exposed ORM with HikariCP Connection Pooling & Persistent H2 Disk Storage
- **Security**: JWT Authentication & BCrypt Hashing

---

## 📁 Repository Structure

```
Trackly/
├── android/                   # Android Jetpack Compose Application
│   ├── app/                   # Main Application Entrypoint & Navigation
│   ├── core/                  # Core Libraries & Feature-Agnostic Modules
│   │   ├── model/             # Shared Data Models & Enums
│   │   ├── network/           # Retrofit & Ktor HTTP API Clients & SessionManager
│   │   ├── location/          # Android Location Foreground Service & LocationClient
│   │   ├── websocket/         # Ktor Client WebSockets Engine
│   │   └── common/            # Design System Tokens & LiveTrackingMapView
│   └── feature/               # Domain Feature Modules
│       ├── auth/              # Login & Registration Screens
│       ├── customer-tracking/ # Active Delivery Tracking UI & Timeline
│       └── driver-delivery/   # Driver Portal & Status Action Pipeline
└── backend/                   # Ktor Kotlin Backend Server Monolith
    └── src/main/kotlin/com/trackly/
        ├── core/              # Database Factory & Security Config
        └── features/          # Server Features (Auth, Orders, Driver, Tracking WebSockets)
```

---

## 🚦 Quick Start Guide

### 1. Run the Backend Server
```bash
cd backend
./gradlew run
```
The server will start listening at `http://0.0.0.0:8080`.

### 2. Forward ADB Port & Run Android App
Connect your Android physical device or start an emulator, then execute:
```bash
adb reverse tcp:8080 tcp:8080
cd android
./gradlew assembleDebug
```
Install the generated APK onto your device (`app/build/outputs/apk/debug/app-debug.apk`).
