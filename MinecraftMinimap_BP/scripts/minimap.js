import { world, system } from "@minecraft/server";

const MAP_RADIUS = 5;
const UPDATE_TICKS = 20;
const RESOURCE_SCAN_TICKS = 60;
const ICON_BASE = 0xE200;
let tick = 0;
const resourceCache = new Map();

function icon(index) {
  return String.fromCharCode(ICON_BASE + index);
}

function terrainStyle(block) {
  if (!block) return ["§8", "·"];
  const id = block.typeId.toLowerCase();
  if (id.includes("water")) return ["§9", "≈"];
  if (id.includes("lava") || id.includes("magma")) return ["§6", "♨"];
  if (id.includes("ice")) return ["§b", "❄"];
  if (id.includes("sand")) return ["§e", "░"];
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

function entityIcon(entity) {
  const id = (entity.typeId || "").toLowerCase();
  if (id.includes("player")) return icon(0);
  if (id.includes("villager")) return icon(1);
  if (id.includes("creeper")) return icon(2);
  if (id.includes("zombie") || id.includes("husk") || id.includes("drowned")) return icon(3);
  if (id.includes("skeleton") || id.includes("stray") || id.includes("bogged")) return icon(4);
  if (id.includes("spider") || id.includes("cave_spider")) return icon(5);
  if (id.includes("cow")) return icon(6);
  if (id.includes("pig")) return icon(7);
  if (id.includes("sheep")) return icon(8);
  if (id.includes("chicken")) return icon(9);
  if (id.includes("horse") || id.includes("donkey") || id.includes("mule")) return icon(10);
  if (id.includes("wolf")) return icon(11);
  if (id.includes("enderman")) return icon(12);
  if (id.includes("witch")) return icon(13);
  if (id.includes("slime") || id.includes("magma_cube")) return icon(14);
  if (id.includes("bee")) return icon(15);
  if (id.includes("iron_golem")) return icon(16);
  if (id.includes("rabbit")) return icon(17);
  if (id.includes("fox")) return icon(18);
  if (id.includes("phantom") || id.includes("pillager") || id.includes("guardian") ||
      id.includes("blaze") || id.includes("ghast") || id.includes("vindicator") ||
      id.includes("evoker") || id.includes("ravager") || id.includes("warden")) return icon(19);
  if (id.includes("item")) return "§f*";
  return icon(19);
}

function resourceInfo(id) {
  if (id.includes("ancient_debris")) return { name: "Debris", glyph: 28 };
  if (id.includes("diamond")) return { name: "Diamond", glyph: 20 };
  if (id.includes("emerald")) return { name: "Emerald", glyph: 23 };
  if (id.includes("gold")) return { name: "Gold", glyph: 22 };
  if (id.includes("iron")) return { name: "Iron", glyph: 21 };
  if (id.includes("copper")) return { name: "Copper", glyph: 24 };
  if (id.includes("redstone")) return { name: "Redstone", glyph: 26 };
  if (id.includes("lapis")) return { name: "Lapis", glyph: 27 };
  if (id.includes("coal")) return { name: "Coal", glyph: 25 };
  if (id.includes("quartz")) return { name: "Quartz", glyph: 29 };
  return undefined;
}

function scanResources(player) {
  const found = new Map();
  const p = player.location;
  const dim = player.dimension;
  const bx = Math.floor(p.x), by = Math.floor(p.y), bz = Math.floor(p.z);
  for (let x = -4; x <= 4; x++) {
    for (let z = -4; z <= 4; z++) {
      for (let y = -5; y <= 3; y++) {
        try {
          const block = dim.getBlock({ x: bx + x, y: by + y, z: bz + z });
          if (!block) continue;
          const info = resourceInfo(block.typeId.toLowerCase());
          if (!info) continue;
          const distance = Math.round(Math.sqrt(x * x + y * y + z * z));
          const old = found.get(info.name);
          if (!old || distance < old.distance) found.set(info.name, { ...info, distance });
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
      if (!entityCells.has(key)) entityCells.set(key, entityIcon(entity));
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
          row += marker + "§r";
          continue;
        }
        try {
          const style = terrainStyle(dim.getTopmostBlock({ x: cx + dx, z: cz + dz }));
          row += style[0] + style[1] + "§r";
        } catch {
          row += "§8·§r";
        }
      }
      rows.push(row);
    }

    const resources = resourceCache.get(player.id) || new Map();
    const priority = ["Diamond", "Debris", "Emerald", "Gold", "Iron", "Copper", "Redstone", "Lapis", "Coal", "Quartz"];
    const resourceText = [];
    for (const name of priority) {
      const item = resources.get(name);
      if (item) resourceText.push(icon(item.glyph) + " §f" + name + " §7" + item.distance + "m");
      if (resourceText.length >= 3) break;
    }
    const resourceLine = resourceText.length
      ? "§6ORES §r" + resourceText.join("  §8|  §r")
      : "§7ORES: none detected nearby";
    const entityCount = Math.max(0, entities.length - 1);
    const dimensionName = dim.id.replace("minecraft:", "").toUpperCase();
    const header = "§l§bSURVIVAL MAP§r  §7N ↑  " + dimensionName;
    const footer = "§7X " + cx + "  Z " + cz + "  §fNearby: " + entityCount;
    player.onScreenDisplay.setActionBar(
      header + "\n" + rows.join("\n") + "\n" + resourceLine + "\n" + footer
    );
  } catch {
    // The player may be unavailable while changing dimensions or leaving.
  }
}

system.runInterval(() => {
  tick += UPDATE_TICKS;
  for (const player of world.getAllPlayers()) {
    if (tick % RESOURCE_SCAN_TICKS === 0) scanResources(player);
    renderMap(player);
  }
}, UPDATE_TICKS);
