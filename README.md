# Trackly — Real-Time Android Delivery & Live Package Tracking App

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green.svg?logo=android)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean%20%2B%20Multi--Module-purple.svg)
![RealTime](https://img.shields.io/badge/RealTime-WebSockets-red.svg)
![AI](https://img.shields.io/badge/AI-Google%20Gemini-sparkles.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

> **Trackly** is a feature-packed, native Android application engineered by **Yash Jaint**. Built using **MVVM (Model-View-ViewModel)** and **Multi-Module Clean Architecture**, Trackly provides a seamless real-time logistics experience for both customers and drivers, featuring live Google Maps tracking, 60fps vehicle marker smoothing, automated address search suggestions, dynamic route ETAs, an AI-powered assistant (Gemini API), and profile management with active delivery deletion safeguards.

---

## 👨‍💻 Author & Original Ownership Notice

**Trackly** was conceptualized, designed, and developed by **Yash Jaint**.

- **Author**: Yash Jaint
- **GitHub**: [@yashjaint](https://github.com/yashjaint)
- **LinkedIn**: [Yash Jaint](https://www.linkedin.com/in/yash-jaint/)
- **Copyright**: © 2026 Yash Jaint. Licensed under the [MIT License](LICENSE).

---

## ✨ Core Features & Application Portals

Trackly offers two distinct portals tailored for Customers and Drivers:

### 📦 Customer Portal
- **Order Creation with Address Autocomplete**: Easily create delivery orders with real-time pickup and delivery location suggestions powered by OpenStreetMap search autocomplete.
- **Live Google Maps Tracking**: Watch your assigned driver move on the map in real time with custom pickup/dropoff markers, route polyline paths, and dynamic arrival ETA calculations.
- **Stage-by-Stage Delivery Timeline**: Track your order through all lifecycle stages (`CREATED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY_FOR_PICKUP` $\rightarrow$ `PICKED_UP` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`).
- **Order History**: Browse active orders or swipe between multiple ongoing orders and review past delivery history.

### 🚚 Driver Portal
- **Job Acceptance & Status Control**: View available delivery jobs, accept assignments, and update delivery milestones step-by-step.
- **Background Location Broadcasting**: Broadcast live driver GPS coordinates to tracking customers via a persistent Android Foreground Service.
- **Delivery Job History**: View past completed delivery records.

### 🤖 Integrated Gemini AI Assistant
- Interactive bottom-sheet AI assistant allowing customers to ask natural language questions about their package (e.g., *"Where is my package right now?"*), powered by Google Gemini API.

### 👤 Account Management & Safety Safeguards
- **Profile Customization**: Edit your Full Name and Vehicle Number (for drivers) via a 3-dot overflow menu.
- **Conditional Account Deletion**: Smart safeguards block account deletion if a customer or driver has active deliveries in progress. Account deletion is only permitted when no active orders exist.

---

## 📱 Android Technology & Architecture Stack

Trackly is engineered following modern Android development guidelines:

| Layer | Technologies & Libraries Used |
| :--- | :--- |
| **UI Framework** | Jetpack Compose, Material Design 3, Custom Glassmorphism UI tokens |
| **UI Pattern** | **MVVM (Model-View-ViewModel)** with `StateFlow` and `Coroutines` |
| **Architecture** | **Clean Architecture** (Domain Repositories & Data Implementations) |
| **Modularization** | **Multi-Module Gradle** (`:feature:customer-tracking`, `:feature:driver-delivery`, `:feature:ai-assistant`, `:feature:auth`, `:core:network`, `:core:location`, `:core:websocket`, `:core:model`, `:core:common`) |
| **Dependency Injection** | Dagger Hilt |
| **Maps & Location** | Google Maps SDK for Android, FusedLocationProviderClient, Android Foreground Service |
| **Real-Time Data** | WebSockets Client (`TrackingWebSocketClient.kt`), Retrofit, OkHttp3 |
| **AI Integration** | Google Gemini API (Firebase AI Logic / REST) |

---

## 📁 Android Module Structure

```
Trackly/
├── android/
│   ├── app/                   # Navigation Host & Hilt Application Entrypoint
│   ├── feature/               # UI Feature Modules (MVVM)
│   │   ├── customer-tracking/ # Active Tracking, Order Timeline & Address Autocomplete
│   │   ├── driver-delivery/   # Driver Job Portal & Status Management
│   │   ├── ai-assistant/      # Gemini AI Chatbot Sheet
│   │   └── auth/              # Login & Registration Screens
│   └── core/                  # Core Layer (Domain / Data)
│       ├── network/           # Domain Repositories & Data Impl Classes
│       ├── location/          # Foreground Service & Location Provider
│       ├── websocket/         # Real-Time WebSocket Location Engine
│       ├── model/             # Domain Models & Enums
│       └── common/            # Design System, Google Maps View & Dialogs
├── LICENSE                    # MIT Copyright License (Yash Jaint)
└── README.md                  # Project Documentation
```

---

## 🚀 Quick Start & Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yashjaint/Trackly.git
   cd Trackly/android
   ```
2. Open `android/` in Android Studio.
3. Build and install the debug APK on your device:
   ```bash
   ./gradlew installDebug
   ```

---

## 📜 License & Copyright

Copyright © 2026 **Yash Jaint**.  
This project is released under the [MIT License](LICENSE).
