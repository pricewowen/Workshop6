# Workshop 6 — Bakery E-Commerce Mobile App

## Overview

**Peelin' Good Bakery (Mobile)** is an Android e-commerce application built in Java.  
Customers can browse products, view details and reviews, add items to a cart, earn loyalty
points, and place pickup or delivery orders. Staff and admins have access to moderation tools
such as photo approvals, staff chat, and account deactivation.

This workshop focuses on:
- Building a multi-screen Android app with the **Navigation Component** and bottom navigation.
- Consuming the **Workshop 7 Spring REST API** (Retrofit) for products, orders, chat, and accounts.
- Implementing authentication, session security, rewards, cart, and checkout flows.

## Tech Stack

- Android Studio
- Java
- AndroidX Navigation Component
- Retrofit / OkHttp / Gson
- Material Components for Android

**Workshop 7 backend** must be running (or reachable) for login, catalog, and orders. The app reads **`API_BASE_URL` from Gradle**: default is emulator `http://10.0.2.2:8080/`. For a **physical device**, add to **`local.properties`** (same folder as `settings.gradle.kts`): `api.base.url=http://YOUR_LAN_IP:8080/` then **Sync Project**. Demo users match the **Workshop 7** seed data (see below).

## Getting Started

### 1. Open the project

1. Clone the repository.
2. Open the **`Workshop6`** project in Android Studio.
3. Let Gradle sync and download dependencies.
4. If Gradle reports **SDK location not found**, copy `local.properties.example` to `local.properties` and set `sdk.dir` to your Android SDK path (Android Studio can also generate `local.properties` when you open the project).

### 2. Run the app

1. Choose an emulator or physical Android device.
2. Run the `app` configuration.
3. The app will launch to the **Login** screen.

Ensure the **Workshop 7** API is up and seeded so the following role accounts exist. Business data is server-managed.

## Demo login credentials (Workshop 7 seed)

Use seeded accounts from Workshop 7:

- Customer password: `Cust123!`
- Employee password: `Emp123!`
- Admin password: `Admin123!`

You can also create new customer accounts with the **Register** link on the login screen.

New user passwords must now meet the app's stronger policy:
- at least 8 characters
- at least 1 uppercase letter
- at least 1 lowercase letter
- at least 1 number
- at least 1 symbol

## Main Screens and Flow

### Authentication

- **`LoginActivity`**
  - Sign in with email or username and password.
  - Successful login creates an encrypted session in `SessionManager`.
  - Deactivated accounts cannot log in.
  - Repeated failed logins trigger a temporary lockout / backoff.

- **`RegisterActivity`**
  - Creates a new `User` and linked `Customer`.
  - Validates user details, address, optional profile photo, and stronger password rules.

- **`EditProfileActivity`**
  - Lets users update profile data and change their password.
  - Saving profile changes requires password re-authentication.
  - Password changes require the current password and enforce the stronger password policy.

### Main Navigation

`MainActivity` hosts a `NavHostFragment` and bottom navigation.

Customer-facing tabs:
- **Home**
- **Browse**
- **Map**
- **Me**
- **Cart**

Staff / admin tabs:
- **Photo Approvals**
- **Accounts** (deactivate / reactivate accounts)
- **Staff Chat**

### Commerce Flow

1. Login or register
2. Browse bakery products
3. Open product details and reviews
4. Add items to cart
5. Open cart and proceed to checkout
6. Choose pickup or delivery
7. Review the order
8. Large orders require password confirmation before placing
9. Place the order and earn reward points

## Data Layer

The app talks to the **Workshop 7 Spring API** via **Retrofit** (`ApiClient` / `ApiService`). Catalog, locations, orders, chat, rewards, and accounts are loaded and updated over the network. Local state is limited to **encrypted session prefs**, **in-memory cart**, and **profile images** saved under app files when needed.

In-app models such as `Product`, `Category`, and `BakeryLocationDetails` are plain Java objects used by the UI and mappers.

## Security Notes

- Password hashing and verification are handled by the Workshop 7 backend.
- Session state is stored using **`EncryptedSharedPreferences`**.
- Sessions now expire after inactivity, with stricter timeout rules for staff/admin users.
- Failed login attempts are tracked and temporarily locked out after repeated failures.
- Registration and password change use a stronger password policy.
- Sensitive actions use **re-authentication**, including profile saves and high-value checkout.
- **Change password** (edit profile) calls **`PUT /api/v1/account/password`** on the Workshop 7 API with the current and new password.
- Staff-only features re-check the logged-in user's role using the API (`getEmployeeMe()` / `getCustomerMe()` as appropriate).
- Logging is sanitized to avoid writing full email addresses, usernames, or address details.
- Staff and admins can deactivate or reactivate accounts from the **Accounts** tab.

### SQL and Input Safety

- Persistence and queries run on the **server** (Spring/JPA); the app does not ship a local business-data store.
- Client-side helpers such as **`SearchUtils`** still escape `LIKE` wildcards (`%`, `_`, and `\`) where search strings are normalized before use.
- Free-text fields are length-limited to reduce abuse and oversized payloads:
  - chat messages: 500 characters
  - checkout order comments: 250 characters
  - search input: 80 characters
- Password handling relies on strong validation and server-side hashing, not client-side SQL filtering.
- **Unit tests** cover search normalization, password rules, and length limits (`SearchUtilsTest`, `Validation`).

## Testing

This project includes both **local unit tests** and **Android instrumented tests**.

### Local unit tests

Local unit tests run on the development machine and are best for utility logic such as:
- password-strength validation
- search normalization and wildcard escaping
- free-text length limiting

Run them with:

```bash
./gradlew.bat testDebugUnitTest
```

### Android instrumented tests

Instrumented tests compile and run on a device or emulator for UI or Android-component checks
(additional instrumented tests can be added as needed).

Compile the Android test APK with:

```bash
./gradlew.bat assembleDebugAndroidTest
```

Run instrumented tests on an emulator or device with:

```bash
./gradlew.bat connectedDebugAndroidTest
```

### Full verification commands

To compile the app and test sources together:

```bash
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest
```
