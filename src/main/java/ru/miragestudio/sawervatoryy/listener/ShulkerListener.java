package ru.miragestudio.sawervatoryy.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.shulker.ShulkerManager;
import ru.miragestudio.sawervatoryy.shulker.ShulkerSession;

/**
 * MirageUShulkers — взаимодействие с шалкерами и защита инвентаря сессии от дюпов.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class ShulkerListener implements Listener {

    private final MirageUShulkers plugin;
    private final ShulkerManager manager;

    public ShulkerListener(MirageUShulkers plugin) {
        this.plugin = plugin;
        this.manager = plugin.shulkers();
    }

    /**
     * Обрабатывается даже если событие уже отменено другим плагином (например, защитой спавна),
     * поэтому ignoreCancelled = false.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null || !manager.isTreasure(block)) {
            return;
        }
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        manager.handleInteract(e.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShulkerSession.Holder holder)) {
            return;
        }
        ShulkerSession session = holder.session();
        if (!(e.getWhoClicked() instanceof Player player) || session == null || session.isClosed()) {
            e.setCancelled(true);
            return;
        }
        if (!session.isOwner(player)) {
            // режим просмотра: ничего нельзя брать, класть и двигать
            e.setCancelled(true);
            Inventory clicked = e.getClickedInventory();
            if (clicked != null && clicked.equals(top)) {
                plugin.messages().actionBar(player, "view-denied");
            }
            return;
        }
        if (!ownerAllowed(e, player, top)) {
            e.setCancelled(true);
        }
    }

    /** Владелец может только забирать предметы из верхнего инвентаря. Класть и дублировать — нельзя. */
    private boolean ownerAllowed(InventoryClickEvent e, Player player, Inventory top) {
        Inventory clicked = e.getClickedInventory();
        if (clicked == null) {
            return true; // клик вне окна (выброс предмета с курсора)
        }
        InventoryAction action = e.getAction();
        if (clicked.equals(top)) {
            ItemStack cursor = e.getCursor();
            boolean cursorEmpty = cursor == null || cursor.getType().isAir();
            if (!cursorEmpty) {
                return false;
            }
            switch (action) {
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, MOVE_TO_OTHER_INVENTORY,
                        DROP_ALL_SLOT, DROP_ONE_SLOT:
                    return true;
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD:
                    if (e.getClick() == ClickType.SWAP_OFFHAND) {
                        return player.getInventory().getItemInOffHand().getType().isAir();
                    }
                    int button = e.getHotbarButton();
                    if (button < 0 || button > 8) {
                        return false;
                    }
                    ItemStack target = player.getInventory().getItem(button);
                    return target == null || target.getType().isAir();
                default:
                    return false;
            }
        }
        // клик в собственном инвентаре: нельзя перекладывать предметы в сессию и собирать их с курсора
        return action != InventoryAction.MOVE_TO_OTHER_INVENTORY && action != InventoryAction.COLLECT_TO_CURSOR;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShulkerSession.Holder holder)) {
            return;
        }
        ShulkerSession session = holder.session();
        boolean owner = e.getWhoClicked() instanceof Player p && session != null && !session.isClosed() && session.isOwner(p);
        if (!owner) {
            e.setCancelled(true);
            return;
        }
        for (int raw : e.getRawSlots()) {
            if (raw < top.getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShulkerSession.Holder holder)) {
            return;
        }
        ShulkerSession session = holder.session();
        if (session == null || session.isClosed() || !(e.getPlayer() instanceof Player player)) {
            return;
        }
        if (session.isOwner(player)) {
            if (manager.finishOnOwnerClose()) {
                manager.finish(session, ShulkerManager.FinishReason.OWNER_CLOSED, false);
            }
        } else {
            session.viewers().remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.onQuit(e.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        manager.onChunkLoad(e.getChunk());
    }
}
