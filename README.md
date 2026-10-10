# Casa Vista Resort Management System (CVRMS) - Android Client Application

![Android Version](https://img.shields.io/badge/Android-14%20%28API%2034%29-brightgreen)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue)
![Language](https://img.shields.io/badge/Language-Java%20%2F%20Kotlin-orange)
![License](https://img.shields.io/badge/License-Proprietary%20%2F%20Confidential-red)

A native Android client application for the **Casa Vista Resort Management System (CVRMS)**. The app provides seamless management and reservation workflows for resort guests, administrative staff, and super administrators.

---

## 👥 Group Members & Project Team

- **Lead Developer**: Dvn-Code
- **Project Role**: Casa Vista Resort Management System Team

---

## 📂 Repository Structure

| Component | Path / Location | Description |
| :--- | :--- | :--- |
| **README** | `/README.md` | System overview, group members, requirements, and step-by-step setup instructions |
| **Source code** | `/app` | Android application client source code (Activities, Fragments, Models, and Network API connection) |
| **Database script** | `/database/booknest.sql` (and `/database/schema.sql`) | `CREATE TABLE` statements and sample records for PostgreSQL / Supabase |
| **Environment sample** | `/.env.example` & `/local.properties.example` | Required environment variables with placeholder values |
| **Dependencies** | `/requirements.txt` | Packages and tools needed to run the backend API and Android client |

---

## 📋 System Overview & Key Features

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

## 💻 Requirements & Prerequisites Needed

### Android Development Requirements
- **IDE**: Android Studio Hedgehog (2023.1.1) or newer
- **Java Development Kit**: JDK 17
- **Android SDK**: Platform 34 (Android 14.0) with Build-Tools 34.0.0
- **Device / Emulator**: Android 7.0 (API Level 24) or higher

### Backend & Database Requirements
- **Database Engine**: PostgreSQL or Supabase SQL Engine
- **Backend API Runtime**: Python 3.10+ (for FastAPI server) or Node.js
- **Key Dependencies**: Listed in `/requirements.txt`

---

## 🗄️ Database Setup Instructions

To set up the database schema and insert sample test records:

1. Open your PostgreSQL database or [Supabase SQL Editor](https://supabase.com).
2. Open the database script file located at:
   ```
   /database/booknest.sql  (or /database/schema.sql)
   ```
3. Execute the SQL script. This will create the required tables:
   - `users`
   - `catalog_items`
   - `bookings`
   - `announcements`
   - `employees`
   - `history_logs`
4. The script automatically inserts pre-populated sample records (sample users, resort facilities, active bookings, announcements, and employees) so you can test all features immediately.

---

## 🛠️ Step-by-Step Setup & Run Instructions

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
1. Launch **Android Studio**.
2. Select **Open** and navigate to the cloned project root directory.
3. Allow Gradle to synchronize dependencies automatically.

### 4. Build and Run
1. Connect an Android device via USB debugging or launch an Android Virtual Device (Emulator API 24+).
2. Click **Run 'app'** (`Shift + F10` or click the green **Play** button in Android Studio).

---

## 📁 Source Code Structure

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

## 📜 License & Intellectual Property

This software is **Proprietary and Confidential**. All Rights Reserved.

Access is temporarily granted strictly for academic coursework evaluation purposes. Unauthorized copying, distribution, modification, reverse engineering, or illegal exploitation of this codebase is strictly prohibited and subject to civil remedies and criminal prosecution. See the [LICENSE](LICENSE) file for full details.

---

## 🤝 Contributing & Support

Maintained by the **Casa Vista Resort Management System team**. For questions or submission evaluations, contact the repository administrator.
