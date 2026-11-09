# Survival AI Live Minimap — 1.2.0

Minecraft Bedrock 1.26.60.x. Requires the matching Resource Pack; the Behavior Pack manifest links it automatically.

## Visual icon update
The companion Resource Pack includes a 256×256 Bedrock custom glyph atlas at `font/glyph_E2.png`. It contains 30 original pixel-art icons for nearby players, villagers, hostile mobs, animals, and ores. The script uses those glyphs in the Action Bar so entities and detected resources appear as pictures rather than single-letter abbreviations.

## Included detections
- Nearby entities within 16 blocks, grouped by map cell.
- Creatures: player, villager, creeper, zombie, skeleton, spider, cow, pig, sheep, chicken, horse, wolf, enderman, witch, slime, bee, iron golem, rabbit, fox and a general hostile icon.
- Nearby ore scan in a 9×9×9 volume around the player: diamond, iron, gold, emerald, copper, coal, redstone, lapis, quartz and ancient debris.
- Surface terrain preview, north-up, coordinates and dimension.

## Detection limits
This is a local minimap overlay in the Action Bar, not a global radar. It only reports entities returned by the nearby query and ores found in loaded blocks in the scan volume. It does not reveal unloaded chunks or all resources underground. A separate corner-positioned draggable HUD is a later UI task.

## Install
Import the generated `.mcaddon`, then enable **Survival AI - Live Minimap** under Behavior Packs. Its linked icon Resource Pack should be enabled with it. If an older version is installed, disable/remove it before importing the update.
