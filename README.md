# LeadZam - Modern Android CRM Click-to-Call App

**LeadZam** is a modern Android application built for CRM click-to-call integration. It receives incoming phone call requests sent from a browser-based CRM or backend system via **Firebase Cloud Messaging (FCM)** and allows the user to launch the native Android phone dialer with the requested number pre-filled.

---

## 📱 Features

- **Modern Jetpack Compose & Material 3 UI**: Clean, responsive layout with support for light and dark themes.
- **FCM Data Message Processing**: Handles structured FCM data payloads (`dial_request`) safely without crashing on missing or malformed fields.
- **Robust Phone Number Validation**: Sanitizes and validates phone numbers (E.164 standard) before displaying or dialing.
- **Foreground & Background Flow**:
  - **Foreground**: Directly presents an **Incoming Call Request Card** with contact name, phone number, and CRM source label.
  - **Background**: Triggers a high-priority Android Notification with custom actions ("Open Dialer").
- **Explicit Native Dialer Handshake**: Uses `Intent.ACTION_DIAL` (`tel:<number>`). **Never** autodials without explicit user interaction and **does not** require `CALL_PHONE` or call log permissions.
- **FCM Token Management**: Displays current device registration token with a one-tap copy-to-clipboard button.
- **Request History & Local Simulator**: Displays current session call requests and provides an in-app simulator for testing without an active FCM backend.

---

## 🏗 Architecture & Message Flow

LeadZam follows modern Android architecture guidelines using Kotlin Coroutines, StateFlow, ViewModel, and a shared Repository state model.

```
[ CRM Backend / Postman ]
          │ (FCM Data Push)
          ▼
[ Firebase Cloud Messaging ]
          │
          ▼
[ CrmFirebaseMessagingService ]
          │
          ├── Validate Payload & Phone Number (PhoneNumberValidator)
          │
          ├── App in Foreground: Update CallRequestRepository -> HomeViewModel -> Jetpack Compose UI
          │
          └── App in Background: Trigger Notification (NotificationHelper)
                                      │
                               (User Taps Notification)
                                      │
                                      ▼
                             [ MainActivity ]
                                      │
                                      ▼
                          [ Native Android Dialer ]
```

### Component Structure
- `data/model/DialRequest.kt`: Data model representing call requests and status (`PENDING`, `DIALED`, `DISMISSED`, `INVALID`).
- `data/validation/PhoneNumberValidator.kt`: Sanitizes formatting and enforces E.164 ITU length constraints (7 to 15 digits).
- `data/repository/CallRequestRepository.kt`: Thread-safe singleton state container holding active request, history, and FCM token.
- `messaging/CrmFirebaseMessagingService.kt`: Listens for FCM push data payloads and token refreshes.
- `messaging/NotificationHelper.kt`: Creates high-priority notification channels and builds pending intents for background alerts.
- `ui/viewmodel/HomeViewModel.kt`: Exposes state to Compose UI and coordinates user actions (copy token, open dialer, dismiss request).
- `ui/screens/HomeScreen.kt` & `ui/components/`: Material 3 Compose screens and component cards (`IncomingCallCard`, `TokenCard`, `StatusCard`, `HistorySection`).

---

## 📩 FCM Data Payload Specification

The backend sends a **data-only** FCM push message with the following JSON structure:

```json
{
  "type": "dial_request",
  "phoneNumber": "+919876543210",
  "contactName": "Rahul Sharma",
  "source": "LeadZam CRM"
}
```

### Field Definitions
| Field | Type | Required | Description |
|---|---|---|---|
| `type` | String | **Yes** | Must be `"dial_request"`. |
| `phoneNumber` | String | **Yes** | Telephone number (E.164 or formatted). Validated before display. |
| `contactName` | String | No | Optional contact display name. Defaults to `"Unknown Contact"`. |
| `source` | String | No | Label showing request origin. Defaults to `"CRM Request"`. |

---

## 🚀 Setup & Firebase Configuration

### Prerequisites
- Android Studio Ladybug (or newer)
- Android Device or Emulator running Android 8.0 (API 26) or higher.
- Java 17 / Kotlin 2.2+

### Firebase Integration Steps
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project or select an existing project.
3. Add an Android Application with Package Name: `com.ashish.leadzam`.
4. Download the `google-services.json` configuration file.
5. Place `google-services.json` into the `app/` directory (`app/google-services.json`), replacing the placeholder file.
6. Build and run the app on a physical device or emulator with Google Play Services.

---

## 🧪 Testing the Application

### 1. Unit Tests
Run unit tests for phone number validation and repository state handling:
```bash
./gradlew app:testDebugUnitTest
```

### 2. Local Demo Simulation (In-App)
1. Launch the LeadZam app.
2. Tap **"Simulate Demo Incoming Call Request"**.
3. An **Incoming Call Request** card will immediately appear at the top.
4. Tap **"Open Dialer"** to verify that the native dialer opens pre-filled with `+919876543210`.

### 3. Testing Real FCM Push Messages
To test FCM messages from cURL or Postman using the Firebase HTTP v1 API:

1. Launch LeadZam on a physical device / Google Play emulator.
2. Tap **"Copy Registration Token"** in the app to copy your FCM device token.
3. Send an HTTP POST request to Firebase FCM:

```bash
curl -X POST https://fcm.googleapis.com/v1/projects/<YOUR-PROJECT-ID>/messages:send \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "<COPIED_DEVICE_FCM_TOKEN>",
      "data": {
        "type": "dial_request",
        "phoneNumber": "+919876543210",
        "contactName": "Rahul Sharma",
        "source": "Web CRM"
      },
      "android": {
        "priority": "HIGH"
      }
    }
  }'
```

---

## 🛡 Foreground & Background Behavior

- **Foreground**: When LeadZam is open, incoming requests update the Compose UI in real time. The user sees a prominent Material 3 card with contact details and action buttons.
- **Background / Closed**: When LeadZam is in the background, a high-priority system notification is posted.
  - Tapping the notification opens LeadZam and exposes the incoming request.
  - Tapping **"Open Dialer"** directly on the notification opens the native dialer pre-filled with the number.

---

## ⚠️ Troubleshooting & Known Limitations

1. **`google-services.json` missing error**: Ensure you place your downloaded `google-services.json` in the `app/` folder.
2. **Notification Permission**: On Android 13+ (API 33+), runtime notification permission is required. Ensure you grant notification permissions when prompted.
3. **No Automatic Calling**: Android security policies strictly forbid automatically initiating phone calls without explicit user consent. LeadZam uses `ACTION_DIAL` so the user always retains control.

---

## 🤖 AI Usage Notes

- **Architecture & Design**: AI was utilized to architect a clean, lifecycle-aware architecture separating FCM payload parsing, repository state management, and Jetpack Compose UI.
- **Validation**: AI implemented `PhoneNumberValidator` and unit tests covering edge cases (E.164, short numbers, malformed strings).
- **Material 3 UI**: AI generated responsive Material 3 Compose components following modern Android design guidelines.
- **Testing & Verification**: Built and verified using Gradle build tasks (`testDebugUnitTest` and `assembleDebug`).
