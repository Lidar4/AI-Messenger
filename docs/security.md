# Security Architecture - AI Messenger

AI Messenger takes a privacy-first approach to all communication, leveraging industry-vetted primitives and strictly avoiding custom, unvetted cryptographic implementations.

## 1. Zero-Plaintext Transmission

All message contents sent via both `InternetTransport` and `NearbyTransport` are fully encrypted before leaving the device. 

- **Algorithm**: AES-GCM (Galois/Counter Mode) with No Padding.
- **Key Derivation**: Secure 256-bit keys derived using `SHA-256` hashing of high-entropy stable secrets.
- **Initialization Vector (IV)**: Securely randomized 12-byte initialization vectors generated fresh per-message. Prepended to the cipherText during transmission.

## 2. Gemini API Credential Protection

In accordance with strict security standards:
- The Gemini API key is **never** hardcoded in the source code or resource strings.
- Key resolution is delegated to `BuildConfig.GEMINI_API_KEY` injected dynamically at build/runtime from the AI Studio platform's secure Secrets manager.
- All network logging is fully disabled in release distributions to prevent transient leaks.

## 3. Replay & Duplicate Attack Mitigation

Our P2P/Nearby Mesh routing layer features strict transaction registers (`MeshRouter` processedMessageIds):
- Message payloads contain immutable, cryptographically random message IDs.
- Duplicate packets received during multicast/gossip routing are instantly evaluated, dropped, and not persisted.
- Route paths expire dynamically after 10 minutes to protect against stale routing loops.
