# AI Messenger

A premium, modern Android messenger featuring an integrated AI Assistant workspace, resilient offline mesh/P2P communication, and industry-standard end-to-end encrypted messaging.

## Main Navigation Elements

1. **Chats**: Active conversations (both one-to-one and groups) with unread count bubbles, message status flags, and timestamps.
2. **Calls**: Log of recent VoIP calls with quick call-back options.
3. **AI Assist Workspace**: Multiple specialized modes (Coding, Debugging, Writing, Research, Study, Data, App Building, Language) powered via the provider-independent `AiProvider` interface connecting to self-hosted or locally controlled AI runtimes (e.g., Ollama).
4. **Settings & Mesh**: Secure control panel to toggle P2P nearby network modes, view discovered nodes on the subnet, view mesh diagnostic logs, and access privacy details.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Local Database**: Room DB (with modern KSP code generation)
- **Networking**: Retrofit + Moshi, TCP ServerSockets for Nearby P2P fallback
- **Encryption**: AES-GCM (Zero-Plaintext transport)
- **AI Integrations**: Provider-independent `AiProvider` abstraction (`OwnAIProvider`) for self-hosted local model runtimes.
- **Workflows**: GitHub Actions CI for Debug APK and release configurations.

---

## First Real AI Test

Follow these exact commands on your self-hosted machine or server to perform your first real AI inference test with the self-hosted AI backend and Ollama.

### 1. Start Ollama and Download Model
```bash
# Start Ollama service
ollama serve &

# Pull the open-weight Llama 3 model
ollama run llama3
```

### 2. Start the Self-Hosted AI Backend
```bash
cd ai-backend
pip install -r requirements.txt
export MODEL_RUNTIME_URL="http://localhost:11434"
export MODEL_NAME="llama3"
export BACKEND_AUTH_TOKEN="secure_messenger_token_123"
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 3. Test `/health` Endpoint
```bash
curl -X GET http://localhost:8000/health
```
Expected response:
```json
{"status":"ok","model":"llama3","runtime":"self-hosted","runtime_url":"http://localhost:11434"}
```

### 4. Send One Real `/v1/chat` Request
```bash
curl -X POST http://localhost:8000/v1/chat \
  -H "Authorization: Bearer secure_messenger_token_123" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello! Translate hello into Bengali.", "system_prompt": "You are a helpful assistant.", "personality": "helpful", "response_length": "balanced"}'
```

### 5. Configure `LOCAL_AI_ENDPOINT`
- **For Android Emulator**: The app defaults to `http://10.0.2.2:8000`, which automatically routes to your host machine's port 8000.
- **For Physical Android Device**: Update `backendBaseUrl` in `OwnAIProvider.kt` to your host machine's local network IP address (e.g., `http://192.168.1.50:8000`).

### 6. Test the Android App
1. Open the Android project in Android Studio or build via `./gradlew assembleDebug`.
2. Navigate to the **AI Workspace** or use message long-press actions (Summarize, Translate, Rewrite).
3. Verify that responses are returned in real-time from your self-hosted backend.
