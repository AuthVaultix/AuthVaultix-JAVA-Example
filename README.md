# AuthVaultix Android SDK Example

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-Android-blue.svg)]()
[![JDK Version](https://img.shields.io/badge/JDK-17%2B-orange.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

A premium, fully featured Java/Android integration example for the **AuthVaultix** authentication platform. This project demonstrates how to connect to the AuthVaultix API, register accounts, login using credentials or licenses, verify sessions, enforce bans, fetch variables, participate in live chats, and gather detailed native system telemetry on Android devices.

---

## Key Features

- 🔐 **Secure Authentication Flows**: Login, registration, license login, upgrades, and password resets.
- ⚙️ **Core SDK Actions**: Session status checks, log submission, variable retrieval/updating, and binary file downloads.
- 💬 **Live Social Features**: Real-time client count, online status listing, and live chat sync (15-second intervals).
- 📱 **System Telemetry Collector**: Captures OS build version, active ABI architecture, runtime threads, and physical memory.
- 🎨 **Modern Dark UI**: Flat dark-theme layout with non-intrusive `AlertDialog` popups.

---

## Getting Started

### System Requirements
- **Java Development Kit (JDK) 17+**
- **Android SDK** (Min SDK: 21, Target/Compile SDK: 36)
- **Gradle 8.10+** (Android Gradle Plugin 8.2.2)

### Setup Instructions
1. **Configure SDK Location**: Create a `local.properties` file in the project root directory:
   ```properties
   sdk.dir=C\:\\Users\\royal\\AppData\\Local\\Android\\Sdk
   ```
2. **Configure Gradle JVM in Android Studio**: Open Settings and set the **Gradle JDK** to Java 17+ (Local system path: `D:\security-pach\New folder\temp-jdk\jdk-17.0.10+7`).

---

## Compiling & Building

Compile the debug APK using the local JDK path configuration:

```powershell
$env:JAVA_HOME = "D:\security-pach\New folder\temp-jdk\jdk-17.0.10+7"
.\gradlew.bat clean assembleDebug
```
The output APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

---

## SDK Integration Guide

The core client logic is managed by `AuthVaultixClient.java`. **Note: All SDK network calls must run on a background thread.**

### 1. Initialization
Declare the client as a class field and initialize it inside `onCreate`:
```java
private AuthVaultixClient client;

client = new AuthVaultixClient(
    this,
    "APP-NAME",
    "OWNER-ID",
    "SECRET",
    "1.0"
);
```

---

## API Function Reference

Here are the primary functions provided by `AuthVaultixClient` and how to use them:

### 🔑 SDK Initialization (`init`)
Connects to the server, completes handshakes, gathers local hardware telemetry, and validates application configuration.
```java
// Returns true if initialization succeeded, false otherwise
boolean success = client.init();
```

### 👤 User Authentication (`login`)
Logs a user in using credentials. Initializes user-session parameters upon success.
```java
// Arguments: (username, password)
boolean success = client.login("my_username", "my_password");
if (success) {
    AuthVaultixClient.UserInfo user = client.getCurrentUser();
} else {
    String errorMsg = client.getResponseCollection(); // Returns failure details
}
```

### 📝 User Registration (`register`)
Creates a new user account linked to a specific license key.
```java
// Arguments: (username, password, licenseKey, email)
boolean success = client.register("my_username", "my_password", "LICENSE-XXXX", "user@email.com");
```

### 🔑 License-Only Login (`licenseLogin`)
Authenticates users directly using their license key, without requiring a username/password setup.
```java
// Arguments: (licenseKey)
boolean success = client.licenseLogin("LICENSE-XXXX");
```

### 🔄 Account Upgrade (`upgrade`)
Upgrades an existing user account to a higher tier using a fresh license key.
```java
// Arguments: (username, licenseKey)
boolean success = client.upgrade("my_username", "LICENSE-UPGRADE-XXXX");
```

### 🩺 Session Status (`check`)
Validates if the current logged-in session token is active on the server. Useful for heartbeat checks.
```java
boolean isSessionValid = client.check();
```

### 🚨 Trigger Security Ban (`ban`)
Enforces a hardware/account ban on the user (e.g. upon detecting client tamper, reverse-engineering attempts).
```java
String[] outMsg = new String[1];
// Arguments: (reason, outMsg)
boolean success = client.ban("Cheating detected", outMsg);
```

### 🛡️ Check Blacklist (`checkBlacklist`)
Checks if the current device/HWID is blacklisted on the AuthVaultix console.
```java
String[] outMsg = new String[1];
boolean isBlacklisted = !client.checkBlacklist(outMsg);
```

### 💾 File Downloader (`download`)
Downloads a binary file securely from the developer panel into a byte array.
```java
String[] outMsg = new String[1];
// Arguments: (fileId, outMsg)
byte[] fileBytes = client.download("EC5FF376", outMsg);
```

### 🏷️ Variable Handlers (`getVar` / `setVar` / `getGlobalVar`)
Fetch developer-controlled dynamic data securely from the backend.
```java
// Fetch global configuration variables
String buildPattern = client.getGlobalVar("global_key");

// Fetch account-specific user variable
String vipLevel = client.getVar("level");

// Set/Update account-specific user variable
boolean success = client.setVar("status", "in_game");
```

### 💬 Chat Room Sync (`chatSend` / `chatFetch`)
Participate in live chat channels hosted in the backend.
```java
String[] outMsg = new String[1];
// Send a text message to a channel (e.g. "test")
boolean sendOk = client.chatSend("Hello from Android!", "test", outMsg);

// Fetch chat logs of a channel
List<AuthVaultixClient.ChatMessage> chatLogs = client.chatFetch("test");
```

### 🏷️ Feature Permissions (`checkFeaturePermission`)
Check if the user has subscription-based feature permissions (e.g., VIP module access).
```java
boolean hasVip = client.checkFeaturePermission("VIP");
```