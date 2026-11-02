# Architecture — Alpha 0.1

- UI: Kotlin + Jetpack Compose, Arabic-first.
- Floating panel: Android overlay permission and foreground service.
- AI: Groq OpenAI-compatible Chat Completions endpoint.
- Local credential storage: AES/GCM using Android Keystore for development convenience only.
- Planned screen understanding: MediaProjection consent + on-device OCR. Capture must be user-initiated and visible.
- Planned world data: Bedrock-specific seed map data, gated by the exact game version.

## Security boundary
The API key is decrypted inside the app to make direct requests. This reduces casual exposure at rest but does not make a client-side secret safe against extraction. Public distribution requires a backend proxy with the key stored in server-side secrets.
