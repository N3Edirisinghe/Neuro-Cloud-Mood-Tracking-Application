# 🧠 Neuro Cloud

🌐 **Live Demo:** [neurocloud.netlify.app](https://neurocloud.netlify.app)

A modern **mental wellness & mood tracking** Android application built with Jetpack Compose and Firebase.

---

## 📱 Features

- **Mood Tracking** — Log your daily mood with scores and notes; view trends over time
- **AI Chat Assistant** — Converse with an AI assistant for mental wellness support
- **Insights Dashboard** — View daily & weekly mood averages and activity counts on the home screen
- **Reports** — Generate and view detailed mood reports; scheduled via WorkManager
- **Pro Activities** — Curated wellness activities and mindfulness exercises
- **Client Stats** — Track and visualize client-level mood statistics
- **Biometric Authentication** — Secure login with fingerprint/biometrics
- **Google Sign-In** — One-tap authentication via Google
- **Profile Management** — Update display name and profile photo
- **Dark-themed UI** — Elegant dark mode with animated transitions throughout

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Authentication | Firebase Auth, Google Sign-In, Biometric API |
| Database (Cloud) | Firebase Firestore |
| Database (Local) | SQLite via `DatabaseHelper` |
| Background Work | WorkManager |
| Image Loading | Coil |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## 🏗️ Project Structure

```
app/src/main/java/fm/mrc/cloudassignment/
├── MainActivity.kt              # Entry point
├── NeuroCloudApplication.kt     # Application class (WorkManager init)
├── auth/
│   ├── AuthManager.kt           # Firebase auth state management
│   ├── GoogleSignInHelper.kt    # Google Sign-In integration
│   └── UserAuthUtils.kt         # Auth utility functions
├── components/
│   └── BottomNavBar.kt          # Bottom navigation bar
├── data/
│   ├── ClientActivity.kt        # Client activity data model
│   └── DatabaseHelper.kt        # Local SQLite helper
├── navigation/
│   └── NavGraph.kt              # Compose Navigation graph
├── screens/
│   ├── HomeScreen.kt            # Dashboard with insights & activities
│   ├── LoginScreen.kt           # Login UI
│   ├── SignUpScreen.kt          # Registration UI
│   ├── TrackScreen.kt           # Mood tracking input
│   ├── ChatScreen.kt            # AI chat interface
│   ├── ReportScreen.kt          # Reports viewer
│   ├── ClientStatsScreen.kt     # Client statistics
│   ├── ProActivitiesScreen.kt   # Wellness activities
│   └── SettingsScreen.kt        # User settings & profile
├── ui/theme/
│   ├── Color.kt                 # Color palette
│   ├── Theme.kt                 # Material3 theme
│   ├── Type.kt                  # Typography
│   └── AppIcons.kt              # Custom icon definitions
└── workers/
    ├── ReportWorker.kt          # Background report generation
    └── ReportScheduler.kt       # WorkManager scheduling
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- A Firebase project with **Authentication** and **Firestore** enabled

### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd cloudassignment
   ```

2. **Add Firebase configuration**
   - Download `google-services.json` from your Firebase Console
   - Place it in `app/google-services.json`

3. **Open in Android Studio**
   - Let Gradle sync complete

4. **Run on a device or emulator**
   ```
   Min SDK: API 24 (Android 7.0)
   ```

---

## 🔐 Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Photo capture for profile picture |
| `RECORD_AUDIO` | Voice input support |
| `WRITE_EXTERNAL_STORAGE` | File saving (Android ≤ 8) |
| `USE_BIOMETRIC` | Biometric/fingerprint login |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reports on reboot |
| `FOREGROUND_SERVICE` | Background report generation |

---

## 📦 Key Dependencies

```kotlin
// Jetpack Compose BOM
implementation(platform(libs.androidx.compose.bom))

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Biometric
implementation("androidx.biometric:biometric:1.1.0")

// Google Sign-In
implementation("com.google.android.gms:play-services-auth:20.7.0")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Image loading
implementation("io.coil-kt:coil-compose:2.4.0")
```

---

## 📄 License

This project is for academic/assignment purposes.
