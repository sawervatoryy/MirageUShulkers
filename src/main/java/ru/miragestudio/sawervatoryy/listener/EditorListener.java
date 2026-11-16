package ru.miragestudio.sawervatoryy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.gui.LootEditor;

/**
 * MirageUShulkers — события окна редактора лута.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class EditorListener implements Listener {

    private final MirageUShulkers plugin;

    public EditorListener(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof LootEditor.Holder holder) {
            plugin.editor().handleClick(e, holder);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof LootEditor.Holder) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof LootEditor.Holder holder) {
            plugin.editor().onClose(holder);
        }
    }
}
