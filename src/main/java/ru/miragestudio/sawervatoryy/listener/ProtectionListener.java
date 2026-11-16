package ru.miragestudio.sawervatoryy.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.Location;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.shulker.ShulkerManager;

import java.util.List;

/**
 * MirageUShulkers — защита Шалкеров сокровищ: нельзя сломать, взорвать, сдвинуть поршнем
 * или набить предметами воронкой.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class ProtectionListener implements Listener {

    private final MirageUShulkers plugin;
    private final ShulkerManager manager;

    public ProtectionListener(MirageUShulkers plugin) {
        this.plugin = plugin;
        this.manager = plugin.shulkers();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (manager.isTreasure(e.getBlock())) {
            e.setCancelled(true);
            if (e.getPlayer().hasPermission(MirageUShulkers.PERM_ADMIN)) {
                plugin.messages().actionBar(e.getPlayer(), "break-hint");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (manager.hasAny()) {
            e.blockList().removeIf(manager::isTreasure);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (manager.hasAny()) {
            e.blockList().removeIf(manager::isTreasure);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (containsTreasure(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (containsTreasure(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if (manager.isTreasure(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopper(InventoryMoveItemEvent e) {
        if (!manager.hasAny()) {
            return;
        }
        Location dest = e.getDestination().getLocation();
        Location src = e.getSource().getLocation();
        if (manager.isTreasure(dest) || manager.isTreasure(src)) {
            e.setCancelled(true);
        }
    }

    private boolean containsTreasure(List<Block> blocks) {
        if (!manager.hasAny()) {
            return false;
        }
        for (Block b : blocks) {
            if (manager.isTreasure(b)) {
                return true;
            }
        }
        return false;
    }
}
