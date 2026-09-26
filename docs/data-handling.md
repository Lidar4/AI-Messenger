# Data Safety Disclosure

For the Google Play Store Data Safety Questionnaire, the actual telemetry, collection, and encryption behaviors of AI Messenger are audited below:

## 1. Data Collection & Transmission

| Data Type | Collected | Shared | Purpose |
|---|---|---|---|
| **Personal Messages** | NO | NO | Transmitted only between endpoints. Never logged or saved on external administrative databases. |
| **Audio/Voice Recordings** | NO | NO | Captured locally only on active request to compile voice messages or VoIP calls. |
| **Device/Network IDs** | NO | NO | Purely utilized for local discovery signaling. Never shared with external trackers. |

## 2. Encryption Standards
- **Data in Transit**: Every payload transmitted over both Nearby Sockets and Retrofit REST API is encrypted using secure `AES-GCM` before leaving the device.
- **Data at Rest**: Local cache and chat database logs are stored in private Room storage databases, inaccessible to other standard applications.

## 3. Account Deletion Support
- Users can clear all locally cached logs, conversations, and account records directly via the Settings tab in one tap.
