import { world, system } from "@minecraft/server";

const MAP_RADIUS = 5;             // 11 x 11 terrain cells
const UPDATE_TICKS = 20;          // once per second
const RESOURCE_SCAN_TICKS = 60;   // heavier underground scan every 3 seconds
const PIXEL = "·";
let tick = 0;
const resourceCache = new Map();

function terrainStyle(block) {
  if (!block) return ["§8", "·"];
  const id = block.typeId.toLowerCase();

  if (id.includes("water")) return ["§9", "≈"];
  if (id.includes("lava") || id.includes("magma")) return ["§6", "♨"];
  if (id.includes("ice")) return ["§b", "❄"];
  if (id.includes("sand") || id.includes("sandstone")) return ["§e", "░"];
  if (id.includes("snow")) return ["§f", "✳"];
  if (id.includes("leaves") || id.includes("azalea") || id.includes("moss")) return ["§a", "♣"];
  if (id.includes("log") || id.includes("wood") || id.includes("planks")) return ["§6", "▥"];
  if (id.includes("flower")) return ["§d", "✿"];
  if (id.includes("grass") || id.includes("fern") || id.includes("vine")) return ["§2", "♧"];
  if (id.includes("dirt") || id.includes("mud") || id.includes("podzol")) return ["§6", "▒"];
  if (id.includes("stone") || id.includes("deepslate") || id.includes("tuff")) return ["§7", "▪"];
  if (id.includes("air")) return ["§8", "·"];
  return ["§7", "▫"];
}

function entitySymbol(entity) {
  const id = (entity.typeId || "").toLowerCase();
  if (id.includes("player")) return ["§b", "P"];
  if (id.includes("villager")) return ["§e", "V"];
  if (id.includes("item")) return ["§f", "*"];
  if (id.includes("creeper")) return ["§a", "C"];
  if (id.includes("zombie") || id.includes("skeleton") || id.includes("spider") ||
      id.includes("witch") || id.includes("slime") || id.includes("phantom") ||
      id.includes("enderman") || id.includes("pillager") || id.includes("drowned") ||
      id.includes("husk") || id.includes("stray")) return ["§c", "!"];
  if (id.includes("cow") || id.includes("pig") || id.includes("sheep") ||
      id.includes("chicken") || id.includes("horse") || id.includes("cat") ||
      id.includes("wolf") || id.includes("rabbit") || id.includes("fox") ||
      id.includes("bee") || id.includes("goat") || id.includes("turtle")) return ["§a", "a"];
  return ["§d", "?"];
}

function resourceName(id) {
  if (id.includes("ancient_debris")) return "Ancient Debris";
  if (id.includes("diamond")) return "Diamond";
  if (id.includes("emerald")) return "Emerald";
  if (id.includes("gold")) return "Gold";
  if (id.includes("iron")) return "Iron";
  if (id.includes("copper")) return "Copper";
  if (id.includes("redstone")) return "Redstone";
  if (id.includes("lapis")) return "Lapis";
  if (id.includes("coal")) return "Coal";
  if (id.includes("quartz")) return "Quartz";
  return undefined;
}

function scanResources(player) {
  const found = new Map();
  const p = player.location;
  const dim = player.dimension;
  const bx = Math.floor(p.x), by = Math.floor(p.y), bz = Math.floor(p.z);

  // A compact 9x9x9 scan around the player; never loads distant chunks.
  for (let x = -4; x <= 4; x++) {
    for (let z = -4; z <= 4; z++) {
      for (let y = -5; y <= 3; y++) {
        try {
          const block = dim.getBlock({ x: bx + x, y: by + y, z: bz + z });
          if (!block) continue;
          const name = resourceName(block.typeId.toLowerCase());
          if (!name) continue;
          const distance = Math.round(Math.sqrt(x * x + y * y + z * z));
          const old = found.get(name);
          if (!old || distance < old.distance) found.set(name, { distance, x, y, z });
        } catch {}
      }
    }
  }
  resourceCache.set(player.id, found);
}

function renderMap(player) {
  try {
    const p = player.location;
    const dim = player.dimension;
    const cx = Math.floor(p.x), cz = Math.floor(p.z);
    const entities = dim.getEntities({ location: p, maxDistance: 16 });
    const entityCells = new Map();

    for (const entity of entities) {
      if (entity.id === player.id) continue;
      const dx = Math.round(entity.location.x - cx);
      const dz = Math.round(entity.location.z - cz);
      if (Math.abs(dx) > MAP_RADIUS || Math.abs(dz) > MAP_RADIUS) continue;
      const key = dx + "," + dz;
      // Keep the first nearby entity in a cell to avoid unreadable overlap.
      if (!entityCells.has(key)) entityCells.set(key, entitySymbol(entity));
    }

    const rows = [];
    for (let dz = -MAP_RADIUS; dz <= MAP_RADIUS; dz++) {
      let row = "";
      for (let dx = -MAP_RADIUS; dx <= MAP_RADIUS; dx++) {
        if (dx === 0 && dz === 0) {
          row += "§c◆§r";
          continue;
        }
        const marker = entityCells.get(dx + "," + dz);
        if (marker) {
          row += marker[0] + marker[1] + "§r";
          continue;
        }
        try {
          const top = dim.getTopmostBlock({ x: cx + dx, z: cz + dz });
          const style = terrainStyle(top);
          row += style[0] + style[1] + "§r";
        } catch {
          row += "§8·§r";
        }
      }
      rows.push(row);
    }

    const dimensionName = dim.id.replace("minecraft:", "").toUpperCase();
    const resources = resourceCache.get(player.id) || new Map();
    const priority = ["Diamond", "Ancient Debris", "Emerald", "Gold", "Iron", "Copper", "Redstone", "Lapis", "Coal", "Quartz"];
    const resourceText = [];
    for (const name of priority) {
      const item = resources.get(name);
      if (item) resourceText.push(name + " " + item.distance + "m");
      if (resourceText.length >= 3) break;
    }
    const resourceLine = resourceText.length
      ? "§6ORES §r" + resourceText.join("  §7|  §r")
      : "§7ORES: none detected nearby";
    const entityCount = Math.max(0, entities.length - 1);
    const header = "§l§bSURVIVAL MAP§r  §7N ↑  " + dimensionName;
    const legend = "§7◆ You  §c! Hostile  §aAnimals  §eV Villager  §bP Player";
    const footer = "§7X " + cx + "  Z " + cz + "  §fMobs: " + entityCount;
    player.onScreenDisplay.setActionBar(
      header + "\n" + rows.join("\n") + "\n" + legend + "\n" + resourceLine + "\n" + footer
    );
  } catch {
    // Player may be unavailable while changing dimensions or leaving.
  }
}

system.runInterval(() => {
  tick += UPDATE_TICKS;
  for (const player of world.getAllPlayers()) {
    if (tick % RESOURCE_SCAN_TICKS === 0) scanResources(player);
    renderMap(player);
  }
}, UPDATE_TICKS);
