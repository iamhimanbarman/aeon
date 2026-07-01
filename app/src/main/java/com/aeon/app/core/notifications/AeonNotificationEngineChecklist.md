# Aeon Notification Engine Checklist

This checklist verifies Aeon’s full notification engine before connecting it deeply with tasks, habits, focus, mood, health, finance, and AI insights.

---

## 1. Core Engine Files

Required files:

- `AeonNotificationContract.kt`
- `AeonNotificationChannels.kt`
- `AeonNotificationPermissionManager.kt`
- `AeonNotificationPublisher.kt`
- `AeonNotificationScheduler.kt`
- `AeonNotificationWorker.kt`
- `AeonNotificationReceiver.kt`
- `AeonNotificationBootReceiver.kt`
- `AeonNotificationRepository.kt`
- `AeonNotificationRuleEngine.kt`
- `AeonNotificationCenter.kt`
- `AeonNotificationDeepLinkHandler.kt`
- `AeonNotificationActionHandler.kt`
- `AeonNotificationInitializer.kt`

Expected responsibility:

- Contract defines types, payloads, rules, schedules, preferences, and history records.
- Channels create Android notification channels safely.
- Permission manager checks notification permission, channel state, and exact alarm access.
- Publisher builds and posts notifications only.
- Scheduler decides scheduling backend: immediate, WorkManager, or AlarmManager.
- Worker publishes WorkManager reminders.
- Receiver publishes AlarmManager reminders and handles notification events.
- Boot receiver restores channels and scheduled rules.
- Repository stores rules, preferences, and notification history.
- Rule engine decides whether to deliver, delay, skip, or suppress.
- Center is the single public API for app features.
- Deep link handler converts notification tap intents into navigation events.
- Action handler handles snooze, open, dismiss, mark done, start focus, and log mood.
- Initializer bootstraps the full system from `Application`.

---

## 2. UI Files

Required notification UI files:

- `NotificationSettingsScreen.kt`
- `NotificationInboxScreen.kt`
- `NotificationPreferenceScreen.kt`
- `AeonNotificationViewModel.kt`
- `AeonNotificationPermissionRequester.kt`
- `AeonNotificationTestPanel.kt`

Expected UI behavior:

- Settings screen controls master preference, categories, quiet hours, digest, daily limit, and channel health.
- Inbox screen shows delivered, scheduled, opened, dismissed, suppressed, and failed notifications.
- Preference screen controls one notification channel/category in detail.
- ViewModel acts as the presentation bridge between Compose UI and `AeonNotificationCenter`.
- Permission requester handles Android 13+ runtime permission flow.
- Test panel verifies publishing, scheduling, exact alarm, WorkManager, default rules, and history.

---

## 3. Gradle Dependencies

Required dependencies:

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.1")

implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
implementation("androidx.lifecycle:lifecycle-process:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```
