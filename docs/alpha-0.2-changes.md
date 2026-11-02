# SurvivalAI 0.2.0-alpha

## Implemented in this update branch
- Preserved package/application ID: `com.survivalai`.
- Increased Android versionCode to 2 and versionName to 0.2.0-alpha.
- Added a custom vector launcher icon and a launch splash background.
- Reworked the home screen with a dark survival-themed visual design.
- Added a direct launch button for Minecraft Bedrock (package `com.mojang.minecraftpe`).
- Rebuilt the floating panel with draggable header, quick-action buttons, Groq responses, return-to-game, minimize, and close actions.
- Quick actions send predefined gameplay prompts to Groq without requiring the player to type each time.

## Still not implemented
- Live screenshot capture and vision-model analysis. The current analyze action clearly reports that this capability is pending; it does not pretend to analyze the game.
- Automatic HUD/coordinate reading.
- Image-based farm/build guides.
- Signed release APK. The update must be signed with the same signing key as the installed APK. A debug-signed build cannot update an app installed from a different signing key.
- The Groq key remains a user-entered local development key. For public distribution, route requests through a backend secret.

## Build
Run `gradle assembleDebug` with Android SDK 35 and JDK 17.
