package ru.miragestudio.sawervatoryy.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.loot.LootEntry;
import ru.miragestudio.sawervatoryy.util.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MirageUShulkers — GUI-редактор лута (/mushulker editloot <1-5>).
 * <ul>
 *   <li>Перетащите предмет курсором в верхнюю область (или Shift+клик в своём инвентаре) — он добавится в лут.</li>
 *   <li>ЛКМ / ПКМ — шанс +5% / −5%, Shift+ЛКМ / Shift+ПКМ — +1% / −1%.</li>
 *   <li>Клавиши 1–4 над предметом — min−1 / min+1 / max−1 / max+1.</li>
 *   <li>Q — удалить запись.</li>
 * </ul>
 *
 * @author MirageStudio, sawervatoryy
 */
public final class LootEditor {

    public static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 47;
    private static final int SLOT_SAVE = 49;
    private static final int SLOT_STATS = 51;
    private static final int SLOT_NEXT = 53;

    /** Holder окна редактора. */
    public static final class Holder implements InventoryHolder {
        private final int level;
        private int page;
        private Inventory inventory;

        private Holder(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final MirageUShulkers plugin;
    private final Set<Holder> open = new HashSet<>();

    public LootEditor(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int level) {
        Holder holder = new Holder(level);
        String titleRaw = plugin.getConfig().getString("gui.editor-title", "&#00ACFF[ ᴘᴇдᴀᴋᴛоᴘ луᴛᴀ | {level} уᴘ. ]");
        Component title = Text.parse(titleRaw.replace("{level}", String.valueOf(level)));
        holder.inventory = Bukkit.createInventory(holder, 54, title);
        open.add(holder);
        render(holder);
        player.openInventory(holder.inventory);
    }

    public void onClose(Holder holder) {
        open.remove(holder);
        plugin.loot().saveIfDirty();
    }

    /** Закрывает все окна редактора и сохраняет loot.yml. */
    public void shutdown() {
        for (Holder h : new ArrayList<>(open)) {
            if (h.inventory != null) {
                for (HumanEntity viewer : new ArrayList<>(h.inventory.getViewers())) {
                    viewer.closeInventory();
                }
            }
        }
        open.clear();
        plugin.loot().saveIfDirty();
    }

    // ------------------------------------------------------------------ отрисовка

    private void render(Holder h) {
        Inventory inv = h.inventory;
        inv.clear();
        List<LootEntry> list = plugin.loot().entries(h.level);
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) PAGE_SIZE));
        h.page = Math.max(0, Math.min(h.page, pages - 1));

        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = h.page * PAGE_SIZE + i;
            if (idx < list.size()) {
                inv.setItem(i, display(list.get(idx)));
            }
        }
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, "&8 ", List.of());
        filler.editMeta(m -> m.setHideTooltip(true));
        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            inv.setItem(slot, filler);
        }
        String levelName = plugin.lockpicks().get(h.level) == null ? "" : plugin.lockpicks().get(h.level).name();
        inv.setItem(SLOT_PREV, button(Material.ARROW, "&#00ACFF« Назад", List.of("&7Страница &f" + (h.page + 1) + "&8/&f" + pages)));
        inv.setItem(SLOT_NEXT, button(Material.ARROW, "&#00ACFFВперёд »", List.of("&7Страница &f" + (h.page + 1) + "&8/&f" + pages)));
        inv.setItem(SLOT_SAVE, button(Material.EMERALD, "&aСохранить &7(loot.yml)", List.of(
                "&7Изменения применяются сразу,",
                "&7а в файл пишутся при закрытии.",
                "",
                "&#00ACFF▸ &fНажмите, чтобы сохранить сейчас")));
        inv.setItem(SLOT_STATS, button(Material.CHEST, "&#00ACFFУровень " + h.level + " &8| &f" + levelName, List.of(
                "&#00ACFF▸ &fЗаписей лута: &#00ACFF" + list.size(),
                "&#00ACFF▸ &fСлотов: &#00ACFF" + plugin.lockpicks().get(h.level).minItems() + "-" + plugin.lockpicks().get(h.level).maxItems(),
                "&#00ACFF▸ &fКрасный шалкер: &#FF7B00" + Text.num(plugin.lockpicks().get(h.level).redChance()) + "%")));
        inv.setItem(SLOT_INFO, button(Material.BOOK, "&#00ACFFКак пользоваться", List.of(
                "&#00ACFF▸ &fПеретащите предмет &7— добавить в лут",
                "&#00ACFF▸ &fShift+клик по своему предмету &7— добавить",
                "&#00ACFF▸ &fлᴋᴍ &7/ &fпᴋм &7— шанс &a+5% &7/ &c−5%",
                "&#00ACFF▸ &fShift+лᴋᴍ &7/ &fпᴋм &7— шанс &a+1% &7/ &c−1%",
                "&#00ACFF▸ &fКлавиши &71 &7/ &72 &7— мин. количество &c−1 &7/ &a+1",
                "&#00ACFF▸ &fКлавиши &73 &7/ &74 &7— макс. количество &c−1 &7/ &a+1",
                "&#00ACFF▸ &fQ &7— удалить запись")));
    }

    private ItemStack display(LootEntry e) {
        ItemStack d = e.item().clone();
        d.setAmount(Math.max(1, Math.min(e.minAmount(), Math.max(1, d.getMaxStackSize()))));
        d.editMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Text.item(""));
            lore.add(Text.item("&#00ACFF▸ &fШанс: &#00ACFF" + Text.num(e.chance()) + "%"));
            lore.add(Text.item("&#00ACFF▸ &fКоличество: &#00ACFF" + e.minAmount() + "&7-&#00ACFF" + e.maxAmount()));
            lore.add(Text.item(""));
            lore.add(Text.item("&7лᴋᴍ&8/&7пᴋм &8• &fшанс &a+5%&8/&c−5%"));
            lore.add(Text.item("&7Shift+лᴋᴍ&8/&7пᴋм &8• &fшанс &a+1%&8/&c−1%"));
            lore.add(Text.item("&71&8/&72 &8• &fмин &c−&8/&a+  &73&8/&74 &8• &fмакс &c−&8/&a+"));
            lore.add(Text.item("&7Q &8• &cудалить запись"));
            meta.lore(lore);
        });
        return d;
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.item(name));
        if (!lore.isEmpty()) {
            meta.lore(Text.itemLines(lore));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    // ------------------------------------------------------------------ клики

    public void handleClick(InventoryClickEvent e, Holder h) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission(MirageUShulkers.PERM_ADMIN)) {
            player.closeInventory();
            return;
        }
        Inventory top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        boolean inTop = raw >= 0 && raw < top.getSize();

        if (!inTop) {
            ItemStack current = e.getCurrentItem();
            if (e.getClickedInventory() != null && e.isShiftClick() && current != null && !current.getType().isAir()) {
                addEntry(h, current, player);
            }
            return;
        }

        if (raw >= PAGE_SIZE) {
            if (raw == SLOT_PREV) {
                h.page--;
                render(h);
            } else if (raw == SLOT_NEXT) {
                h.page++;
                render(h);
            } else if (raw == SLOT_SAVE) {
                plugin.loot().save();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
                plugin.messages().actionBar(player, "editor-saved");
            }
            return;
        }

        ItemStack cursor = e.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            addEntry(h, cursor, player);
            return;
        }

        List<LootEntry> list = plugin.loot().entries(h.level);
        int idx = h.page * PAGE_SIZE + raw;
        if (idx < 0 || idx >= list.size()) {
            return;
        }
        LootEntry entry = list.get(idx);
        ClickType click = e.getClick();
        boolean changed = true;
        switch (click) {
            case LEFT -> entry.setChance(entry.chance() + 5);
            case RIGHT -> entry.setChance(entry.chance() - 5);
            case SHIFT_LEFT -> entry.setChance(entry.chance() + 1);
            case SHIFT_RIGHT -> entry.setChance(entry.chance() - 1);
            case DROP, CONTROL_DROP -> list.remove(idx);
            case NUMBER_KEY -> {
                switch (e.getHotbarButton()) {
                    case 0 -> entry.setMinAmount(entry.minAmount() - 1);
                    case 1 -> entry.setMinAmount(entry.minAmount() + 1);
                    case 2 -> entry.setMaxAmount(entry.maxAmount() - 1);
                    case 3 -> entry.setMaxAmount(entry.maxAmount() + 1);
                    default -> changed = false;
                }
            }
            default -> changed = false;
        }
        if (changed) {
            plugin.loot().markDirty();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
            render(h);
        }
    }

    private void addEntry(Holder h, ItemStack source, Player player) {
        double defChance = plugin.getConfig().getDouble("editor.default-chance", 10.0);
        int amount = Math.max(1, source.getAmount());
        List<LootEntry> list = plugin.loot().entries(h.level);
        LootEntry entry = new LootEntry(source, defChance, amount, amount);
        entry.setChance(defChance);
        list.add(entry);
        h.page = Math.max(0, (list.size() - 1) / PAGE_SIZE);
        plugin.loot().markDirty();
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
        render(h);
    }
}
