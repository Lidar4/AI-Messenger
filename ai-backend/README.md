# Self-Hosted AI Backend for AI Messenger

This backend provides a secure, private, self-hosted AI proxy connecting the Android AI Messenger to an open-weight local model runtime (Ollama / Llama 3).

## Architecture

```
Android AI Messenger ──(HTTPS)──> FastAPI Backend ──> Ollama Runtime ──> Open-Weight Model (Llama 3)
```

## Setup & Startup Instructions

### 1. Prerequisites
- Docker & Docker Compose installed on your self-hosted server or machine.

### 2. Start Services
Run Docker Compose:
```bash
docker-compose up -d
```

### 3. Pull the Open-Weight Model
Inside the Ollama container, pull `llama3` (or your preferred model):
```bash
docker exec -it ollama_runtime ollama run llama3
```

### 4. Endpoints Overview

- `GET /health` - Check backend and model runtime status.
- `POST /v1/chat` - Chat completion with personality and length options.
- `POST /v1/translate` - Multilingual translation (English/Bengali).
- `POST /v1/summarize` - Summarization.
- `POST /v1/group/chat` - Group `@AI` context-aware chat.
- `POST /v1/creative` - Creative Studio / Birthday surprise JSON artifact generation.
