package ru.miragestudio.sawervatoryy.shulker;

import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MirageUShulkers — активная сессия открытого Шалкера сокровищ.
 * Один общий инвентарь: владелец (потративший отмычку) может забирать лут, остальные — только смотреть.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class ShulkerSession {

    /** Holder виртуального инвентаря — по нему слушатели находят сессию. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private ShulkerSession session;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public ShulkerSession session() {
            return session;
        }
    }

    private final BlockKey key;
    private final UUID owner;
    private final String ownerName;
    private final int level;
    private final boolean red;
    private final Holder holder;
    private final long expiresAt;
    private final Set<UUID> viewers = new HashSet<>();

    private ShulkerBox box;
    private BukkitTask task;
    private boolean closed;

    public ShulkerSession(BlockKey key, Player owner, int level, boolean red, Holder holder, long expiresAt) {
        this.key = key;
        this.owner = owner.getUniqueId();
        this.ownerName = owner.getName();
        this.level = level;
        this.red = red;
        this.holder = holder;
        this.expiresAt = expiresAt;
        holder.session = this;
    }

    public BlockKey key() {
        return key;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean isOwner(Player player) {
        return owner.equals(player.getUniqueId());
    }

    public int level() {
        return level;
    }

    public boolean isRed() {
        return red;
    }

    public Holder holder() {
        return holder;
    }

    public Inventory inventory() {
        return holder.getInventory();
    }

    public long expiresAt() {
        return expiresAt;
    }

    public Set<UUID> viewers() {
        return viewers;
    }

    public ShulkerBox box() {
        return box;
    }

    public void setBox(ShulkerBox box) {
        this.box = box;
    }

    public BukkitTask task() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public boolean isClosed() {
        return closed;
    }

    public void markClosed() {
        this.closed = true;
    }
}
