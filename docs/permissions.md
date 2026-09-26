# Permission Justification Audit - AI Messenger

We strictly adhere to the principle of **least-privilege**, requesting only standard or dangerous permissions that are strictly vital to the requested user experiences.

## Permissions Declared

| Permission | Scope | Technical Justification |
|---|---|---|
| `android.permission.INTERNET` | Normal (Install-time) | Required to transmit messages over `InternetTransport` and call the Gemini API REST endpoints. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal (Install-time) | Required to dynamically query active network capabilities (`ConnectivityManager`) to route messages intelligently. |
| `android.permission.RECORD_AUDIO` | Dangerous (Runtime) | Explicitly requested only when the user triggers voice message recording or activates call managers. No background audio is captured. |

## Removed/Omitted Permissions

- **Location Permissions (`ACCESS_FINE_LOCATION`)**: Removed. Subnet scanning and P2P routing are handled securely using pure IP Socket transport on local networks without broad location scanning.
- **Broad Storage (`READ_EXTERNAL_STORAGE`)**: Removed. Zero-permission Photo Picker or internal caches are utilized, fully satisfying modern Android target limits.
- **SMS/Call Log/Contacts Permissions**: Avoided completely to eliminate user friction.
