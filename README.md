# JY Forward — Android 13 (API 33) Smart Dual-SIM SMS Webhook Forwarder

**JY Forward** is a standalone Android mobile application written in **Kotlin** using **Jetpack Compose (Material 3)** and **MVVM Architecture**. It listens for incoming SMS messages across both SIM cards (`android.provider.Telephony.SMS_RECEIVED`) and forwards matching messages to user-defined Webhook URLs using a persistent **Foreground Service** (with a sticky notification and partial WakeLock for Doze mode reliability) **without** replacing the user's default SMS application.

---

## Core Architecture & Features

1. **Standalone Background Service & BroadcastReceiver**:
   - `SmsBroadcastReceiver`: Listens to `android.provider.Telephony.SMS_RECEIVED` (priority `999`), reconstructs multi-part SMS PDUs, extracts Dual-SIM subscription & slot IDs (`subscription`, `slot`, `SubscriptionManager`), and hands off work to `SmsForwardingService`.
   - `SmsForwardingService`: Runs as a `START_STICKY` Foreground Service (`foregroundServiceType="dataSync"`) with an ongoing notification and a 30-second `PARTIAL_WAKE_LOCK` so webhooks succeed even when the device screen is locked or in Doze mode.
   - `BootReceiver`: Automatically starts `SmsForwardingService` upon `BOOT_COMPLETED` or `MY_PACKAGE_REPLACED`.

2. **Runtime Permissions & Battery Optimization (Android 13 / API 33)**:
   - Requests `RECEIVE_SMS`, `READ_SMS`, `READ_PHONE_STATE`, and `POST_NOTIFICATIONS` at runtime.
   - Prompts for `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to prevent OEM/Doze background restrictions.

3. **Room Database Filter System (`FilterRuleEntity`)**:
   - Filter by **Sender Number/Contact** (`*` for wildcard) and **Case-Insensitive Keyword** (e.g., `واریز`).
   - Supports **Dual-SIM Filtering** (`Both SIMs`, `SIM 1 Only`, `SIM 2 Only`).
   - Supports **GET** and **POST** HTTP methods via **OkHttp**.
   - Full CRUD + **Clone Rule**, **Enable/Disable Toggle**, **Rule Summary View**, and **One-Tap Test Webhook**.
   - Pre-seeded with the JomYekta Card-to-Card webhook template:
     ```
     https://www.jomyekta.ir/index.php?option=com_osmembership&task=payment_confirm&payment_method=os_cardtocard&cardtocard_action=webhook&token=%24143356%40Aliapalipz&msg={msg}
     ```
     *(Automatically replaces `{msg}` with the UTF-8 URL-encoded SMS body).*

4. **History Section (`HistoryLogEntity`)**:
   - Logs **only** messages that triggered an active filter rule, including SIM slot, HTTP status code, resolved webhook URL, and server response summary.
   - Includes a **Clear All** button.

5. **UI/UX, Localization, Security & Backup**:
   - **Bottom Navigation Bar**: `Filters`, `History`, `Settings`.
   - **3 Themes**: `Light`, `Dark`, `System Default` + **Adjustable Font Size Scale** (`85%` to `135%`).
   - **Bilingual (English & Persian/Farsi)**: Full RTL/LTR support (`values/strings.xml` and `values-fa/strings.xml`), defaulting to the system language with in-app switcher.
   - **Biometric App Lock**: Uses `androidx.biometric:biometric` (`BIOMETRIC_STRONG | BIOMETRIC_WEAK | DEVICE_CREDENTIAL`) on app launch.
   - **Backup & Restore**: Exports and imports all Room DB Filter Rules + Settings as a structured `.json` file via Android Storage Access Framework (SAF).
   - **About Us**: Displays app name, version, and clickable links to [www.jomyekta.ir](https://www.jomyekta.ir) and [www.jomyekta.com](https://www.jomyekta.com).

---

## Project Structure

```text
JYForward/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/ir/jomyekta/jyforward/
        │   ├── JYForwardApplication.kt
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── local/
        │   │   │   ├── AppDatabase.kt
        │   │   │   ├── FilterRuleDao.kt
        │   │   │   ├── FilterRuleEntity.kt
        │   │   │   ├── HistoryLogDao.kt
        │   │   │   ├── HistoryLogEntity.kt
        │   │   │   └── SettingsDataStore.kt
        │   │   ├── network/
        │   │   │   └── WebhookDispatcher.kt
        │   │   └── repository/
        │   │       └── ForwardRepository.kt
        │   ├── receiver/
        │   │   ├── BootReceiver.kt
        │   │   └── SmsBroadcastReceiver.kt
        │   ├── service/
        │   │   └── SmsForwardingService.kt
        │   ├── ui/
        │   │   ├── screens/
        │   │   │   ├── FiltersScreen.kt
        │   │   │   ├── HistoryScreen.kt
        │   │   │   └── SettingsScreen.kt
        │   │   ├── theme/
        │   │   │   └── JYForwardTheme.kt
        │   │   └── viewmodel/
        │   │       └── MainViewModel.kt
        │   └── util/
        │       ├── BackupJsonManager.kt
        │       └── BiometricLockHelper.kt
        └── res/
            ├── drawable/ic_jy_forward_logo.xml
            ├── values/strings.xml
            ├── values/themes.xml
            ├── values-fa/strings.xml
            └── xml/
                ├── backup_rules.xml
                ├── data_extraction_rules.xml
                └── locales_config.xml
```

## How to Open & Build in Android Studio

1. Extract `JY_Forward_Android_Project.zip`.
2. Open **Android Studio Hedgehog (2023.1.1) or newer**.
3. Select **Open** and choose the `JYForward` folder.
4. Let Android Studio sync Gradle and download the Android SDK 33/34 dependencies.
5. Run on a physical Android 13+ Dual-SIM device or emulator (`Run 'app'`).
