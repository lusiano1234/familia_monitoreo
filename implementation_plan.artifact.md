# Implementation Plan: Chat Content Capture and In-App View

The user wants the application to capture and display messages from within chat applications (like WhatsApp or Instagram), rather than just relying on notification snippets. This requires enhancing the Accessibility Service to perform screen scraping and creating a new UI to view the captured logs.

## User Review Required

> [!IMPORTANT]
> **Privacy & Ethics**: Capturing full chat content is a high-permission task. Ensure that this is used within the legal framework of parental supervision or device monitoring. The app already requires Accessibility permissions, which will be used for this feature.

## Proposed Changes

### 1. Enhanced Message Capture (Screen Scraping)

#### [MODIFY] [BackupCaptureService.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/BackupCaptureService.kt)
- Update `onAccessibilityEvent` to listen for `TYPE_WINDOW_CONTENT_CHANGED` and `TYPE_WINDOW_STATE_CHANGED`.
- Implement a screen crawler that identifies chat windows from monitored packages (WhatsApp, Telegram, etc.).
- Extract text from message nodes using `AccessibilityNodeInfo` traversal.
- Add deduplication logic to avoid capturing the same screen state multiple times.

### 2. Local Data Persistence

#### [NEW] [MessageLogHelper.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/MessageLogHelper.kt)
- A utility to save captured messages to a local file or SharedPreferences (initially a simple JSON list for easier implementation) so they can be displayed in the app.

### 3. Chat Log UI

#### [NEW] [ChatLogActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/ChatLogActivity.kt)
- A new activity to display a list of captured messages, including source app, sender, text, and timestamp.

#### [NEW] [activity_chat_log.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_chat_log.xml)
- Layout for the message log using a `RecyclerView`.

#### [MODIFY] [StatusActivity.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/StatusActivity.kt)
- Add a button to open the `ChatLogActivity`.

#### [MODIFY] [activity_status.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/res/layout/activity_status.xml)
- Add a new button "VER MENSAJES CAPTURADOS" in the UI.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/AndroidManifest.xml)
- Register the new `ChatLogActivity`.

## Verification Plan

### Manual Verification
1.  Deploy the app and enable Accessibility Service (Lector de Respaldo).
2.  Open WhatsApp and enter a chat.
3.  Scroll through some messages.
4.  Return to the Family Monitor app and open "VER MENSAJES CAPTURADOS".
5.  Verify that the messages seen on the WhatsApp screen are listed in the log.
