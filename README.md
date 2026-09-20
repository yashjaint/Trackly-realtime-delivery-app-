# Trackly — Real-Time Android Delivery & Live Package Tracking App

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green.svg?logo=android)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean%20%2B%20Multi--Module-purple.svg)
![DI](https://img.shields.io/badge/DI-Dagger%20Hilt-brightgreen.svg)
![Concurrency](https://img.shields.io/badge/Concurrency-Coroutines%20%2B%20StateFlow-orange.svg)
![Database](https://img.shields.io/badge/Database-Room-darkgreen.svg)
![Networking](https://img.shields.io/badge/API-Retrofit%20%2B%20REST-blueviolet.svg)
![RealTime](https://img.shields.io/badge/RealTime-WebSockets-red.svg)
![AI](https://img.shields.io/badge/AI-Google%20Gemini-sparkles.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

> **Trackly** is a production-grade native Android application for real-time package delivery and live logistics tracking engineered by **Yash Jaint**. Built using **Multi-View View Model (MVVM)** pattern and **Multi-Module Clean Architecture with Domain Use Cases**, Trackly leverages **Dagger Hilt Dependency Injection**, **Kotlin Coroutines**, **StateFlow**, **Room Database**, **Retrofit REST API Integration**, **Google Maps SDK**, and **Google Gemini AI Integration** to deliver a seamless real-time tracking experience for both customers and delivery drivers.

---

## 👨‍💻 Author & Original Ownership Notice

**Trackly** was conceptualized, designed, and developed by **Yash Jaint**.

- **Author**: Yash Jaint
- **GitHub**: [@yashjaint](https://github.com/yashjaint)
- **LinkedIn**: [Yash Jaint](https://www.linkedin.com/in/yash-jaint/)
- **Copyright**: © 2026 Yash Jaint. Licensed under the [MIT License](LICENSE).

---

## ✨ Core Features & Application Portals

Trackly provides tailored experiences for both Customers and Delivery Drivers:

### 📦 Customer Portal
- **Order Creation with Address Autocomplete**: Create delivery orders with real-time pickup and delivery location suggestions powered by OpenStreetMap search autocomplete.
- **Live Google Maps Tracking**: Watch your assigned delivery driver move on the map in real time with custom pickup/dropoff markers, route polyline paths, and dynamic arrival ETA calculations.
- **Stage-by-Stage Delivery Timeline**: Track your package through all lifecycle stages (`CREATED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY_FOR_PICKUP` $\rightarrow$ `PICKED_UP` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`).
- **Order History & Active Cards**: Swipe between active ongoing orders or review past order history.

### 🚚 Delivery Driver Portal
- **Job Acceptance & Milestone Control**: View available delivery jobs, accept assignments, and update package delivery status milestones step-by-step.
- **Background Location Broadcasting**: Broadcast live driver GPS coordinates to tracking customers via a persistent Android Foreground Service.
- **Delivery Job History**: View past completed delivery records.

### 🤖 Integrated Gemini AI Assistant
- Interactive bottom-sheet AI assistant allowing customers to ask natural language questions about their package (e.g., *"Where is my package right now?"*), powered by Google Gemini API.

### 👤 Profile & Account Management
- **Profile Customization**: View and edit your Full Name and Vehicle Number via a 3-dot overflow menu.
- **Account Deletion Safeguards**: Account deletion rules check for active orders or delivery jobs before processing.

---

## 📱 Android Technology & Architecture Stack

Trackly is engineered following modern Android development guidelines:

| Layer | Technologies & Frameworks Used |
| :--- | :--- |
| **UI Framework** | Jetpack Compose, Material Design 3, Custom Glassmorphism UI tokens |
| **UI Pattern** | **Multi-View View Model (MVVM)** with `StateFlow` and `Coroutines` |
| **Architecture** | **Clean Architecture** (Domain Layer **Use Cases** & Repositories + Data Layer Implementations) |
| **Modularization** | **Multi-Module Gradle** (`:feature:customer-tracking`, `:feature:driver-delivery`, `:feature:ai-assistant`, `:feature:auth`, `:core:network`, `:core:location`, `:core:websocket`, `:core:database`, `:core:model`, `:core:common`) |
| **Dependency Injection** | Dagger Hilt |
| **Local Persistence** | **Room Database** (Entities, DAOs, Local Caching) |
| **Maps & Location** | Google Maps SDK for Android, FusedLocationProviderClient, Android Foreground Service |
| **Networking & APIs** | **Retrofit**, OkHttp3 REST API Integration, Ktor WebSockets Client |
| **AI Integration** | **Google Gemini API Integration** |

---

## 📁 Android Module & Clean Architecture Layout

```
Trackly/
├── android/
│   ├── app/                   # Navigation Host & Hilt Application Entrypoint
│   ├── feature/               # UI Feature Modules (MVVM + Domain Use Cases)
│   │   ├── customer-tracking/ # Active Tracking, Order Timeline & Address Autocomplete
│   │   ├── driver-delivery/   # Delivery Driver Job Portal & Status Management
│   │   ├── ai-assistant/      # Gemini AI Chatbot Sheet & AskAiAssistantUseCase
│   │   └── auth/              # Login & Register Screens, LoginUseCase & RegisterUseCase
│   └── core/                  # Core Layer (Domain Repositories & Data Implementations)
│       ├── network/           # Retrofit REST API Interfaces & Data Repository Impls
│       ├── database/          # Room Database (DAOs & Local Entities)
│       ├── location/          # Foreground Service & Location Provider
│       ├── websocket/         # Real-Time WebSocket Location Engine
│       ├── model/             # Shared Domain Models & Enums
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
