# Survival AI — Live Minimap Add-on

A first Bedrock Edition minimap prototype for Minecraft 1.26.x.

## What it does
- Reads the actual highest block at each sampled X/Z coordinate using the Bedrock Script API.
- Renders a 9×9 live terrain view in the in-game action-bar HUD.
- Keeps north at the top and marks the player's current tile in red.
- Displays current X/Z coordinates and dimension.
- Refreshes approximately every 0.6 seconds.

## Install
1. Import the generated `.mcaddon` into Minecraft.
2. Open the target world settings.
3. Enable **Survival AI - Live Minimap** under Behavior Packs.
4. Enter the world and allow the script to run.

## Notes
This first build uses the supported action-bar HUD surface, not a custom corner widget. It samples loaded/generated terrain through `Dimension.getTopmostBlock`; unloaded chunks and positions where the API cannot read a block are shown dark. The map is a top-down surface map, not cave/mineshaft radar.

The Android app UI remains separate; this add-on runs inside Minecraft itself.
