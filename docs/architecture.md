# Architecture Guide - AI Messenger

AI Messenger is a production-grade, highly resilient messaging application for Android. It prioritizes offline-first reliability, high-performance local mesh/P2P communication, and an integrated, secure AI assistant workspace.

## Core Topology

```
             ┌────────────────────────┐
             │       Compose UI       │
             └───────────┬────────────┘
                         ▼
             ┌────────────────────────┐
             │     ChatViewModel      │
             └───────────┬────────────┘
                         ▼
             ┌────────────────────────┐
             │     ChatRepository     │
             └─────┬──────────────┬───┘
                   │              │
                   ▼              ▼
             ┌───────────┐  ┌───────────┐
             │ Local DB  │  │ Transport │
             │  (Room)   │  │  Router   │
             └───────────┘  └─────┬─────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
      ┌───────────────────────┐       ┌───────────────────────┐
      │   InternetTransport   │       │    NearbyTransport    │
      │  (Retrofit REST API)  │       │  (TCP Client/Server)  │
      └───────────────────────┘       └───────────────────────┘
```

## Layers and Package Structures

- **`com.example.ui`**: Modern Jetpack Compose UI layout with 6 central tabs (Chats, Groups, Calls, AI, Settings), strict safe areas (`enableEdgeToEdge`), and distinct touch targets.
- **`com.example.data`**: Persistence architecture using Room, reactive flows (`Flow<T>`), and clean repository pattern (`ChatRepository`) serving as the absolute source of truth.
- **`com.example.domain`**: High-performance interfaces such as `CommunicationTransport` interface.
- **`com.example.network`**: Sockets networking, peer subsystem (`P2PManager`), and message routing (`MeshRouter`).
- **`com.example.security`**: End-to-end payload encryption (`EncryptionHelper`) utilizing standard `AES-GCM-NoPadding` primitives.
- **`com.example.ai`**: Specialized Generative AI clients (`GeminiService`) calling modern `gemini-3.5-flash` model directly via secure REST.
