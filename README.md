# Kafe

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK%2034-3DDC84.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-02569B.svg?style=flat-square)](https://developer.android.com/topic/architecture)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg?style=flat-square)](https://github.com/fauzanabdulhakim/Kafe)
[![Tests](https://img.shields.io/badge/Tests-40%2F40%20Passing-success.svg?style=flat-square)](https://github.com/fauzanabdulhakim/Kafe)
[![Gradle](https://img.shields.io/badge/Gradle-8.9-02303A.svg?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

An enterprise-ready Android food and beverage ordering application engineered with 100% Kotlin, Clean Architecture, MVVM with Unidirectional Data Flow (UDF), and Android Jetpack ViewBinding. The system provides real-time catalog discovery, offline cart persistence, cryptographic price verification, digital checkout, and live order tracking synchronized via Firebase.

---

## Overview

Kafe is designed for consumer-facing restaurant and cafe operations. It eliminates legacy architectural anti-patterns through pure Kotlin domain modeling, strict layer decoupling, and scoped session state management.

The application operates alongside [KafeServer](https://github.com/fauzanabdulhakim/KafeServer), which serves as the kitchen display and administrative management counterpart.

---

## Architecture

The project adheres to Clean Architecture guidelines and MVVM separation of concerns:

```
+--------------------------------------------------------------+
|                      Presentation Layer                      |
|       Activities, Fragments, ViewModels, ViewBinding         |
|             LiveData / StateFlow, EventBus events            |
+------------------------------+-------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                         Domain Layer                         |
|   UseCases (ValidateOrderPlacementUseCase, PriceCalculator)  |
|       Pure Kotlin Domain Models, Repository Interfaces       |
|                  (Zero Android SDK imports)                  |
+------------------------------+-------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                          Data Layer                          |
|         Room Database (CartDAO, LocalCartDataSource)         |
|          Firebase Realtime Database, Firebase Auth           |
|        Firebase Cloud Messaging (FCM), Retrofit 2 APIs       |
+--------------------------------------------------------------+
```

### Architectural Principles

- **Pure Kotlin Domain**: Business rules, pricing calculations, and domain contracts reside in the domain package with zero Android SDK dependencies.
- **Unidirectional Data Flow (UDF)**: View emissions trigger ViewModel actions, which execute UseCases, emitting immutable state to observing Views.
- **Zero Global Mutable State**: Replaced legacy static references with a thread-safe, scoped `ClientSessionManager`.
- **ViewBinding Standards**: Completely replaced ButterKnife and Kotlin synthetics with Android Jetpack ViewBinding. All binding instances are nullified in `onDestroyView()` to prevent memory leaks.
- **Resource Lifecycle Management**: All RxJava streams (`Disposable`) and Firebase database listeners are explicitly managed and cleared upon ViewModel teardown.
- **Layout Optimization**: Layout trees utilize flat `ConstraintLayout` hierarchies, eliminating nested multi-pass weight chains. Adaptive configurations are provided for landscape (`values-land`) and tablet interfaces (`sw600dp`).

---

## Core Capabilities

### Catalog Discovery & Customization
- **Promotional Carousels**: Auto-looping paginated banners showcasing active promotions and best deals.
- **Category Browsing**: Fast catalog filtering with Glide disk-caching and memory optimizations.
- **Menu Item Customization**: Support for dynamic size selections, optional add-ons, user ratings, and reviews.
- **Item Availability (86-ing)**: Visual indicators and input validation preventing out-of-stock items from being placed into active carts.

### Offline-First Cart & Price Protection
- **Local Persistence**: Cart state is managed locally using Android Jetpack Room Database and RxJava2.
- **Price Verification**: Implements `ValidateOrderPlacementUseCase` to validate base prices, add-on totals, quantities, and order subtotals prior to submission, neutralizing client-side data tampering.
- **Decoupled Cart Synchronization**: Real-time cart count updates broadcast across the application via decoupled EventBus contracts.

### Checkout & Order Lifecycle
- **Payment Gateway Flexibility**: Integrates Cash on Delivery (COD), digital wallet flows, and Braintree Drop-in payment processing.
- **Geocoding & Location**: Automatic address resolution using Google Play Services location APIs.
- **State Machine Synchronization**: Order progress is tracked in real time against Firebase Realtime Database across deterministic stages: `Placed (0)`, `Shipping (1)`, `Shipped (2)`, and `Cancelled (-1)`.
- **Refund Inquiries**: Dedicated cancellation and refund submission workflow connecting directly to kitchen staff.
- **Push Alerts**: Foreground and background order dispatch notifications delivered via Firebase Cloud Messaging (FCM).

---

## Tech Stack

| Component | Specification |
|---|---|
| **Language** | Kotlin 1.9.0 (100% Kotlin codebase) |
| **Android SDK** | Minimum SDK 22 (Android 5.1) / Target SDK 34 (Android 14) |
| **Architecture** | Clean Architecture, MVVM, Unidirectional Data Flow (UDF) |
| **Jetpack Libraries** | ViewBinding, ViewModel, LiveData, Room Database, Navigation Component |
| **Reactive Programming** | RxJava 2, RxAndroid |
| **Cloud Services** | Firebase Realtime Database, Firebase Authentication, Firebase Cloud Messaging |
| **Networking** | Retrofit 2, OkHttp 3, Gson Converter |
| **Image Loading** | Bumptech Glide 4 |
| **Unit Testing** | JUnit 4, Mockito, Mockito-Kotlin, AndroidX Architecture Testing Core |

---

## Project Structure

```
Kafe/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fauzan/kafe/
│   │   │   │   ├── Adapter/        # List and carousel adapters
│   │   │   │   ├── Callback/       # Asynchronous callback interfaces
│   │   │   │   ├── Common/         # Session manager, constants, utility classes
│   │   │   │   ├── Database/       # Room entities, DAOs, and local data sources
│   │   │   │   ├── domain/         # Clean Architecture UseCases and business logic
│   │   │   │   ├── EventBus/       # UI event bus models
│   │   │   │   ├── Model/          # Domain entities and data transfer objects
│   │   │   │   ├── Remote/         # Cloud Functions Retrofit client
│   │   │   │   ├── services/       # Firebase Cloud Messaging service
│   │   │   │   ├── ui/             # MVVM feature packages (cart, menu, foodlist, orders)
│   │   │   │   ├── HomeActivity.kt # Main shell activity
│   │   │   │   └── MainActivity.kt # Authentication and startup entrypoint
│   │   │   ├── res/                # Layouts, landscape/tablet dimens, values
│   │   │   └── AndroidManifest.xml
│   │   └── test/                   # JUnit4 and Mockito unit tests
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
└── settings.gradle
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer (Ladybug / Koala recommended)
- JDK 17 (JetBrains Runtime `jbr-17` or OpenJDK 17)
- Android SDK Platform 34 and Build Tools 34.0.0+
- Valid `google-services.json` from the Firebase Console

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone git@github.com:fauzanabdulhakim/Kafe.git
   cd Kafe
   ```

2. **Configure Firebase (Optional for Testing)**
   The repository includes a mock `google-services.json` that enables automated testing and compilation out of the box. To connect to your live Firebase backend:
   - Download your project's `google-services.json` from the [Firebase Console](https://console.firebase.google.com/).
   - Replace `app/google-services.json` (or reference `app/google-services.json.example` as a template).

3. **Open in Android Studio**
   - Open the `Kafe` directory in Android Studio.
   - Verify Gradle JVM points to **JDK 17** under `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM`.
   - Synchronize Gradle dependencies.

4. **Assemble and Run**
   - Select an emulator or connected device running Android API 22 or higher.
   - Execute `./gradlew assembleDebug` or trigger **Run 'app'** from Android Studio.

---

## Quality Assurance & Testing

The repository contains an automated unit test suite verifying pricing calculations, business rules, session isolation, and order validation.

Execute the test suite using Gradle:

```bash
./gradlew testDebugUnitTest
```

### Coverage Highlights

- `ValidateOrderPlacementUseCaseTest`: Validates order integrity, subtotal calculations, and item counts to prevent client-side price tampering.
- `PriceCalculatorTest`: Ensures calculation accuracy across complex configurations of add-ons, sizes, and quantities.
- `ClientSessionManagerTest`: Verifies thread safety and session lifecycle management.
- `OrderStatusMapperTest`: Tests order state transitions across all lifecycle states.
- `FoodModelTest` & `RefundRequestModelTest`: Validates domain models and item availability flags.

---

## Companion Application

This repository represents the customer client of the Kafe platform. For administrative management, order dispatching, and kitchen operations, refer to:

- **[KafeServer](https://github.com/fauzanabdulhakim/KafeServer)**: Merchant and Kitchen Management System.

---

## Contribution Guidelines

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`.
3. Adhere to Clean Architecture standards and accompany business logic with unit tests.
4. Ensure all unit tests pass: `./gradlew testDebugUnitTest`.
5. Submit a pull request with a comprehensive summary of changes.

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
