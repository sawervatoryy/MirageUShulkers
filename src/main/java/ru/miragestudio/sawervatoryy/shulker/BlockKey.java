package ru.miragestudio.sawervatoryy.shulker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * MirageUShulkers — ключ блока (мир + координаты).
 *
 * @author MirageStudio, sawervatoryy
 */
public record BlockKey(String world, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey of(Location loc) {
        return new BlockKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /** Блок или null, если мир не загружен. */
    public Block toBlock() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    /** Блок или null, если мир или чанк не загружены (чанки не подгружаются). */
    public Block toLoadedBlock() {
        World w = Bukkit.getWorld(world);
        if (w == null || !w.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        return w.getBlockAt(x, y, z);
    }

    public Location center() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x + 0.5, y + 0.5, z + 0.5);
    }
}
