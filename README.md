# Casa Vista Resort Management System (CVRMS) - Android Application

![Android Version](https://img.shields.io/badge/Android-14%20%28API%2034%29-brightgreen)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue)
![Language](https://img.shields.io/badge/Language-Java%20%2F%20Kotlin-orange)
![License](https://img.shields.io/badge/License-MIT-green)

A native Android client application for the **Casa Vista Resort Management System (CVRMS)**. The app provides seamless management and reservation workflows for resort guests, administrative staff, and super administrators.

---

## 🌟 Key Features

### 🏨 Guest / Customer Features
- **Account & Authentication**: Email verification, Google Sign-In integration, and profile management.
- **Resort Catalog & Facilities**: View available resort amenities, rooms, and sports facilities (Basketball court, Pickleball court, etc.).
- **Reservations & Bookings**: Real-time availability checking, calendar date pickers, and booking requests.
- **Digital Payments**: Integrated payment dialogs supporting digital payment solutions (e.g., Maya test payments).
- **Announcements**: Live announcements and updates from resort management.

### 🛡️ Admin & Staff Dashboard
- **Booking Management**: Review, approve, or cancel guest reservations.
- **Point of Sale (POS)**: POS terminal interface for handling walk-in orders and instant facility billing.
- **Facility Control**: Toggle availability and set unavailable reasons for facilities.

### 👑 Super Admin Dashboard
- **System Overview**: Comprehensive analytics and resort activity monitoring.
- **Room & Asset Management**: Update room photos, rates, and operational statuses.
- **Employee & Staff Directory**: Manage resort employee credentials and access levels.
- **Audit Logs & History**: View detailed action logs and resort transaction records.

---

## 🚀 Tech Stack & Prerequisites

- **Language**: Java / Kotlin
- **Build System**: Gradle 8.2+
- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 34 (Android 14)
- **Java Version**: JDK 17
- **Backend & Services**:
  - FastAPI / Vercel Serverless Backend (`https://cvrms-api.vercel.app`)
  - Supabase Database & Auth (`https://knobgbjusbcqaatizjld.supabase.co`)
  - Google Play Services Auth (`play-services-auth:21.2.0`)

---

## 🛠️ Getting Started & Local Setup

### 1. Clone the Repository
```bash
git clone https://github.com/Dvn-Code/cvrms_android.git
cd cvrms_android
```

### 2. Configure Local Environment
1. Copy `.env.example` and `local.properties.example` to create your local config files:
   ```bash
   cp .env.example .env
   cp local.properties.example local.properties
   ```
2. Update `local.properties` with the path to your Android SDK:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   ```

### 3. Open in Android Studio
1. Launch **Android Studio** (Hedgehog 2023.1.1 or newer recommended).
2. Select **Open** and navigate to the cloned project folder.
3. Allow Gradle to synchronize dependencies.

### 4. Build and Run
- Connect an Android device or launch an Emulator running API 24+.
- Click **Run 'app'** (`Shift + F10`).

---

## 📁 Project Architecture

```
app/src/main/
├── java/com/example/casavista/
│   ├── adapters/         # RecyclerView Adapters for bookings, catalog, history, etc.
│   ├── admin/            # Admin Activity and Fragments (Bookings, POS, Dashboard, Profile)
│   ├── auth/             # Login, Signup, and Email Verification Activities
│   ├── data/             # Repositories for employee and resort data
│   ├── dialogs/          # Custom Dialogs (Maya payments, reservation popups, etc.)
│   ├── helpers/          # API Helper and Avatar utilities
│   ├── models/           # Data models (Booking, CatalogItem, Announcement, Employee, etc.)
│   ├── network/          # Supabase client and backend API configurations
│   ├── superadmin/       # Super Admin Activity and Fragments
│   └── user/             # Guest/Customer Activity and Fragments
└── res/
    ├── layout/           # XML layout definitions
    ├── drawable/         # Custom vector drawables and assets
    └── values/           # Strings, themes, and color palettes
```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing & Support

Maintained by the **Casa Vista Resort Management System team**. For issues or feature requests, feel free to open an issue or submit a pull request on the [GitHub repository](https://github.com/Dvn-Code/cvrms_android).
