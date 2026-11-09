import { world, system } from "@minecraft/server";

const MAP_RADIUS = 4;       // 9 x 9 blocks
const UPDATE_TICKS = 12;    // refresh about every 0.6 seconds
const PIXEL = "■";

function colorFor(block) {
  if (!block) return "§8";
  const id = block.typeId.toLowerCase();

  if (id.includes("water") || id.includes("ice")) return "§9";
  if (id.includes("lava") || id.includes("magma")) return "§6";
  if (id.includes("sand") || id.includes("sandstone") || id.includes("beach")) return "§e";
  if (id.includes("snow") || id.includes("powder_snow")) return "§f";
  if (id.includes("leaves") || id.includes("azalea") || id.includes("moss")) return "§a";
  if (id.includes("grass") || id.includes("fern") || id.includes("flower")) return "§2";
  if (id.includes("log") || id.includes("wood") || id.includes("planks")) return "§6";
  if (id.includes("stone") || id.includes("deepslate") || id.includes("ore") || id.includes("andesite") || id.includes("diorite") || id.includes("granite")) return "§7";
  if (id.includes("dirt") || id.includes("mud") || id.includes("podzol")) return "§6";
  if (id.includes("air")) return "§8";
  return "§2";
}

function getPixel(dimension, x, z) {
  try {
    const block = dimension.getTopmostBlock({ x, z });
    return colorFor(block) + PIXEL + "§r";
  } catch {
    return "§8" + PIXEL + "§r";
  }
}

function renderMap(player) {
  try {
    const pos = player.location;
    const dimension = player.dimension;
    const centerX = Math.floor(pos.x);
    const centerZ = Math.floor(pos.z);
    const rows = [];

    // North is always at the top. The center tile marks the player.
    for (let dz = -MAP_RADIUS; dz <= MAP_RADIUS; dz++) {
      let row = "";
      for (let dx = -MAP_RADIUS; dx <= MAP_RADIUS; dx++) {
        if (dx === 0 && dz === 0) {
          row += "§c◆§r";
        } else {
          row += getPixel(dimension, centerX + dx, centerZ + dz);
        }
      }
      rows.push(row);
    }

    const dimensionName = dimension.id.replace("minecraft:", "").toUpperCase();
    const header = "§l§bWORLD MAP§r  §7N ↑  " + dimensionName;
    const footer = "§7X " + centerX + "  Z " + centerZ + "  •  §c◆§7 YOU";
    player.onScreenDisplay.setActionBar(header + "\n" + rows.join("\n") + "\n" + footer);
  } catch {
    // A player may briefly be unavailable while changing dimensions or leaving.
  }
}

system.runInterval(() => {
  for (const player of world.getAllPlayers()) {
    renderMap(player);
  }
}, UPDATE_TICKS);
