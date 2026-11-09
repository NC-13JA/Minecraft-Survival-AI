# Survival AI — Enhanced Live Minimap Add-on

Target: Minecraft Bedrock 1.26.60.x, using stable `@minecraft/server` 2.11.0.

## Current features
- 11×11 top-down surface map with distinct terrain symbols for water, lava, ice, sand, snow, trees, plants, dirt and stone.
- Red diamond marks the player; north stays at the top.
- Nearby entities are overlaid on the map: hostile mobs (!), animals (a), villagers (V), dropped items (*) and other entities (?).
- Nearby ores/resources are scanned in a compact 9×9×9 volume around the player. The nearest detected types and approximate block distance are listed (diamond, ancient debris, emerald, gold, iron, copper, redstone, lapis, coal and quartz).
- Coordinates, dimension and nearby entity count are shown.
- Surface map refreshes every second; resource scan refreshes every three seconds.

## Important detection limits
- The map samples the topmost surface block at each X/Z position; it is not a cave map.
- Resource detection only checks blocks in the loaded area within the scan volume. It does not reveal unexplored or unloaded chunks, and it cannot identify every possible item or structure in the world.
- Entity markers show entities returned by the nearby-dimension query; they are not a global radar.
- This version still uses Minecraft's action-bar HUD location. A true draggable, resizable top-right minimap requires a custom Resource Pack HUD screen and a tested way to feed live script data into that UI. This build does not claim that corner-widget interaction is implemented yet.

## Install
1. Import the generated `.mcaddon` into Minecraft.
2. Open the target world settings and enable **Survival AI - Live Minimap** under Behavior Packs.
3. Enter the world. If an older copy is already installed, remove/disable that pack version before importing the updated one.
