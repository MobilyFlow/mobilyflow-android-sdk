# CLAUDE.md - MobilyFlow Android SDK

## Project Overview

MobilyFlow is a SaaS platform that helps mobile developers manage in-app purchases and subscriptions on the App Store and Play Store.

This repository is the **Android Native SDK** - a Kotlin library for integrating MobilyFlow's in-app purchase and subscription management into Android applications.

**Related MobilyFlow components (separate repositories):**
- SDKs: Native (Swift for iOS, Kotlin for Android) and cross-platform (React Native, Flutter)
- Webhook: Sends standardized events about in-app purchases and subscriptions
- API: Automates communication with MobilyFlow

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Min SDK:** 23 | **Compile SDK:** 35
- **Java Version:** 11

**Key Dependencies:**
- Google Play Billing 8.1.0
- OkHttp 4.12.0
- kotlinx-datetime 0.6.2

## Project Structure

```
mobilyflow-android-sdk/
├── mobilyflow-android-sdk/           # Main SDK library module
│   └── src/main/java/com/mobilyflow/mobilypurchasesdk/
│       ├── MobilyPurchaseSDK.kt          # Public singleton facade (entry point)
│       ├── MobilyPurchaseSDKImpl.kt      # Internal implementation
│       ├── MobilyPurchaseAPI/            # API communication layer
│       ├── BillingClientWrapper/         # Google Play Billing integration
│       ├── Models/                       # Data classes (Product, Entitlement, etc.)
│       ├── Enums/                        # Enumeration types
│       ├── Exceptions/                   # Custom exceptions
│       ├── SDKHelpers/                   # Helper classes
│       ├── Utils/                        # Utility functions
│       └── Monitoring/                   # Logging & diagnostics
├── mobilyflow-android-test-sdk/      # Demo/test application
└── gradle/libs.versions.toml         # Dependency versions catalog
```

## Common Commands

```bash
# Build the SDK
./gradlew :mobilyflow-android-sdk:build

# Run tests
./gradlew :mobilyflow-android-sdk:test

# Build test app
./gradlew :mobilyflow-android-test-sdk:assembleDebug

# Clean build
./gradlew clean

# Publish to Maven Local (for local testing)
./gradlew :mobilyflow-android-sdk:publishToMavenLocal
```

## Architecture

- **Singleton Pattern:** `MobilyPurchaseSDK` object is the public facade
- **Internal Implementation:** `MobilyPurchaseSDKImpl` contains core business logic
- **Wrapper Pattern:** `BillingClientWrapper` wraps Google Play Billing Client
- **Thread Safety:** Synchronized blocks for initialization and shared state

**Purchase Flow:**
1. Initialize SDK with `MobilyPurchaseSDK.initialize()`
2. Login customer with `login(externalRef)`
3. Fetch products with `getProducts()` or `getSubscriptionGroups()`
4. Initiate purchase with `purchaseProduct()`
5. SDK handles billing flow and waits for webhook confirmation
6. Entitlements synchronized automatically

## Test App Setup

Create `env.properties` in the root folder:
```properties
MOBILYFLOW_APP_ID=""
MOBILYFLOW_API_KEY=""
MOBILYFLOW_API_URL=""  # For self-hosted only
```

## Publishing

Published to Maven Central via Sonatype:
- **Coordinates:** `com.mobilyflow:mobilyflow-android-sdk`
- **Current Version:** Check `Version.kt`

## Key Files

- `MobilyPurchaseSDK.kt` - Public API surface
- `MobilyPurchaseSDKImpl.kt` - Core implementation (~700 lines)
- `BillingClientWrapper.kt` - Google Play Billing integration
- `MobilyPurchaseAPI.kt` - Backend API communication
- `gradle/libs.versions.toml` - Dependency version management
