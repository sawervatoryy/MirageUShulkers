package ru.miragestudio.sawervatoryy.shulker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.lockpick.LockpickLevel;
import ru.miragestudio.sawervatoryy.util.Effects;
import ru.miragestudio.sawervatoryy.util.Messages;
import ru.miragestudio.sawervatoryy.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MirageUShulkers — ядро механики Шалкера сокровищ: список шалкеров, сессии открытия,
 * Красный шалкер (x2), таймеры, защита от дюпов.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class ShulkerManager {

    public enum FinishReason {
        OWNER_CLOSED, EXPIRED, QUIT, ADMIN, SHUTDOWN
    }

    private final MirageUShulkers plugin;
    private final Set<BlockKey> shulkers = new LinkedHashSet<>();
    private final Map<BlockKey, ShulkerSession> sessions = new HashMap<>();
    private final Map<UUID, ShulkerSession> byOwner = new HashMap<>();
    private final Map<UUID, Long> denyCooldown = new HashMap<>();

    private int sessionSeconds = 60;
    private int inventorySize = 27;
    private boolean finishOnOwnerClose = true;
    private boolean dropLeftover = true;
    private double knockback = 0.6;
    private long denyCooldownMs = 1500;
    private boolean actionbarTimer = true;
    private Material normalMaterial = Material.PURPLE_SHULKER_BOX;
    private Material redMaterial = Material.RED_SHULKER_BOX;

    private int redMultiplier = 2;
    private boolean exArmor = true;
    private boolean exWeapons = true;
    private boolean exTools = false;
    private final Set<Material> exMaterials = new HashSet<>();

    private Effects.EffectSet fxOpen = Effects.EffectSet.EMPTY;
    private Effects.EffectSet fxRed = Effects.EffectSet.EMPTY;
    private Effects.EffectSet fxDeny = Effects.EffectSet.EMPTY;
    private Effects.EffectSet fxClose = Effects.EffectSet.EMPTY;
    private Effects.EffectSet fxAmbient = Effects.EffectSet.EMPTY;
    private Effects.EffectSet fxAmbientRed = Effects.EffectSet.EMPTY;

    public ShulkerManager(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ загрузка

    public void reload() {
        shutdown();
        loadSettings();
        loadShulkers();
    }

    private void loadSettings() {
        FileConfiguration c = plugin.getConfig();
        sessionSeconds = Math.max(5, c.getInt("settings.session-seconds", 60));
        int size = c.getInt("settings.inventory-size", 27);
        inventorySize = Math.max(9, Math.min(54, (size / 9) * 9));
        finishOnOwnerClose = c.getBoolean("settings.finish-on-owner-close", true);
        dropLeftover = "DROP".equalsIgnoreCase(c.getString("settings.leftover-action", "DROP"));
        knockback = Math.max(0.0, c.getDouble("settings.deny-knockback", 0.6));
        denyCooldownMs = Math.max(0, c.getLong("settings.deny-cooldown-ms", 1500));
        actionbarTimer = c.getBoolean("settings.actionbar-timer", true);
        normalMaterial = shulkerMaterial(c.getString("settings.normal-material", "PURPLE_SHULKER_BOX"), Material.PURPLE_SHULKER_BOX);
        redMaterial = shulkerMaterial(c.getString("settings.red-material", "RED_SHULKER_BOX"), Material.RED_SHULKER_BOX);

        redMultiplier = Math.max(2, c.getInt("red-shulker.multiplier", 2));
        exArmor = c.getBoolean("red-shulker.exclude.armor", true);
        exWeapons = c.getBoolean("red-shulker.exclude.weapons", true);
        exTools = c.getBoolean("red-shulker.exclude.tools", false);
        exMaterials.clear();
        for (String name : c.getStringList("red-shulker.exclude.materials")) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                exMaterials.add(m);
            } else {
                plugin.getLogger().warning("red-shulker.exclude.materials: неизвестный материал " + name);
            }
        }

        fxOpen = Effects.load(c, "effects.open", plugin.getLogger());
        fxRed = Effects.load(c, "effects.red", plugin.getLogger());
        fxDeny = Effects.load(c, "effects.deny", plugin.getLogger());
        fxClose = Effects.load(c, "effects.close", plugin.getLogger());
        fxAmbient = Effects.load(c, "effects.ambient", plugin.getLogger());
        fxAmbientRed = Effects.load(c, "effects.ambient-red", plugin.getLogger());
    }

    private Material shulkerMaterial(String name, Material def) {
        Material m = name == null ? null : Material.matchMaterial(name);
        if (m == null || !Tag.SHULKER_BOXES.isTagged(m)) {
            plugin.getLogger().warning("Материал " + name + " не является шалкером, использую " + def.name());
            return def;
        }
        return m;
    }

    private void loadShulkers() {
        shulkers.clear();
        for (Map<?, ?> map : plugin.getConfig().getMapList("shulkers")) {
            try {
                String world = String.valueOf(map.get("world"));
                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();
                shulkers.add(new BlockKey(world, x, y, z));
            } catch (Exception ex) {
                plugin.getLogger().warning("Некорректная запись в shulkers: " + map);
            }
        }
    }

    private void saveShulkers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BlockKey k : shulkers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("world", k.world());
            m.put("x", k.x());
            m.put("y", k.y());
            m.put("z", k.z());
            list.add(m);
        }
        plugin.getConfig().set("shulkers", list);
        plugin.saveConfig();
    }

    // ------------------------------------------------------------------ список шалкеров

    public boolean isTreasure(Block block) {
        return !shulkers.isEmpty() && shulkers.contains(BlockKey.of(block));
    }

    public boolean isTreasure(Location loc) {
        return !shulkers.isEmpty() && loc != null && loc.getWorld() != null && shulkers.contains(BlockKey.of(loc));
    }

    public boolean hasAny() {
        return !shulkers.isEmpty();
    }

    public List<BlockKey> all() {
        return new ArrayList<>(shulkers);
    }

    public boolean isActive(BlockKey key) {
        return sessions.containsKey(key);
    }

    public boolean finishOnOwnerClose() {
        return finishOnOwnerClose;
    }

    /** Привязывает блок шалкера. @return false, если он уже привязан. */
    public boolean add(Block block) {
        BlockKey key = BlockKey.of(block);
        if (!shulkers.add(key)) {
            return false;
        }
        try {
            BlockState state = block.getState(false);
            if (state instanceof Container container) {
                container.getInventory().clear();
            }
        } catch (Exception ignored) {
            // пустой шалкер — ничего чистить
        }
        setMaterialKeepFacing(block, normalMaterial);
        saveShulkers();
        return true;
    }

    /** Отвязывает блок шалкера. @return false, если он не был привязан. */
    public boolean remove(Block block) {
        BlockKey key = BlockKey.of(block);
        ShulkerSession session = sessions.get(key);
        if (session != null) {
            finish(session, FinishReason.ADMIN, false);
        }
        if (!shulkers.remove(key)) {
            return false;
        }
        saveShulkers();
        return true;
    }

    // ------------------------------------------------------------------ нормализация блоков

    public void normalizeLoaded() {
        for (BlockKey key : shulkers) {
            normalize(key);
        }
    }

    public void onChunkLoad(Chunk chunk) {
        if (shulkers.isEmpty()) {
            return;
        }
        String world = chunk.getWorld().getName();
        for (BlockKey key : shulkers) {
            if (key.world().equals(world) && (key.x() >> 4) == chunk.getX() && (key.z() >> 4) == chunk.getZ()) {
                Bukkit.getScheduler().runTask(plugin, () -> normalize(key));
            }
        }
    }

    private void normalize(BlockKey key) {
        if (sessions.containsKey(key)) {
            return;
        }
        Block block = key.toLoadedBlock();
        if (block == null || !Tag.SHULKER_BOXES.isTagged(block.getType())) {
            return;
        }
        if (block.getType() != normalMaterial) {
            setMaterialKeepFacing(block, normalMaterial);
        }
    }

    private void setMaterialKeepFacing(Block block, Material material) {
        BlockFace face = BlockFace.UP;
        if (block.getBlockData() instanceof Directional d) {
            face = d.getFacing();
        }
        if (block.getType() != material) {
            block.setType(material, false);
        }
        if (block.getBlockData() instanceof Directional nd && nd.getFacing() != face && nd.getFaces().contains(face)) {
            nd.setFacing(face);
            block.setBlockData(nd, false);
        }
    }

    // ------------------------------------------------------------------ взаимодействие

    public void handleInteract(Player player, Block block) {
        BlockKey key = BlockKey.of(block);
        ShulkerSession existing = sessions.get(key);
        if (existing != null) {
            openView(player, existing);
            return;
        }
        if (!Tag.SHULKER_BOXES.isTagged(block.getType())) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        int level = plugin.lockpicks().detect(hand);
        if (level < 1) {
            deny(player, block);
            return;
        }
        Messages msg = plugin.messages();
        if (!player.hasPermission(MirageUShulkers.PERM_USE)) {
            msg.send(player, "no-permission");
            return;
        }
        if (byOwner.containsKey(player.getUniqueId())) {
            msg.send(player, "already-active");
            return;
        }
        openNew(player, key, block, level);
    }

    private void deny(Player player, Block block) {
        long now = System.currentTimeMillis();
        Long last = denyCooldown.get(player.getUniqueId());
        if (last != null && now - last < denyCooldownMs) {
            return;
        }
        denyCooldown.put(player.getUniqueId(), now);
        plugin.messages().send(player, "no-key");
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        fxDeny.playSounds(player, center);
        fxDeny.playParticles(center);

        Vector dir = player.getLocation().toVector().subtract(center.toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = player.getLocation().getDirection().multiply(-1);
            dir.setY(0);
        }
        if (knockback > 0 && dir.lengthSquared() > 1.0E-4) {
            dir.normalize().multiply(knockback);
            dir.setY(0.25);
            player.setVelocity(dir);
        }
    }

    public void openView(Player player, ShulkerSession session) {
        if (session.isClosed()) {
            return;
        }
        if (session.isOwner(player)) {
            player.openInventory(session.inventory());
            return;
        }
        if (!player.hasPermission(MirageUShulkers.PERM_USE)) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        session.viewers().add(player.getUniqueId());
        player.openInventory(session.inventory());
        plugin.messages().send(player, "view-start", "player", session.ownerName(), "level", String.valueOf(session.level()));
    }

    private void openNew(Player player, BlockKey key, Block block, int level) {
        Messages msg = plugin.messages();
        LockpickLevel lp = plugin.lockpicks().get(level);
        if (lp == null) {
            return;
        }
        List<ItemStack> loot = plugin.loot().generate(level, lp.minItems(), lp.maxItems(), inventorySize);
        if (loot.isEmpty()) {
            msg.send(player, "no-loot", "level", String.valueOf(level), "level-name", lp.name());
            return;
        }
        boolean red = ThreadLocalRandom.current().nextDouble() * 100.0 < lp.redChance();
        if (red) {
            loot = applyRedBonus(loot);
        }

        // забираем 1 отмычку из руки
        ItemStack main = player.getInventory().getItemInMainHand().clone();
        if (main.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            main.setAmount(main.getAmount() - 1);
            player.getInventory().setItemInMainHand(main);
        }

        String titleRaw = plugin.getConfig().getString(red ? "gui.title-red" : "gui.title", "[ шᴀлᴋᴇᴘ ᴄоᴋᴘовищ | {level} уᴘ. ]");
        Component title = Text.parse(msg.apply(titleRaw, "level", String.valueOf(level), "level-name", lp.name(), "player", player.getName()));

        ShulkerSession.Holder holder = new ShulkerSession.Holder();
        Inventory inventory = Bukkit.createInventory(holder, inventorySize, title);
        holder.setInventory(inventory);
        fill(inventory, loot);

        long expiresAt = System.currentTimeMillis() + sessionSeconds * 1000L;
        ShulkerSession session = new ShulkerSession(key, player, level, red, holder, expiresAt);
        sessions.put(key, session);
        byOwner.put(player.getUniqueId(), session);

        if (red) {
            setMaterialKeepFacing(block, redMaterial);
        }
        try {
            BlockState state = block.getState(false);
            if (state instanceof ShulkerBox box) {
                box.open();
                session.setBox(box);
            }
        } catch (Exception ex) {
            plugin.getLogger().fine("Не удалось открыть крышку шалкера: " + ex.getMessage());
        }

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        fxOpen.play(center);
        if (red) {
            fxRed.play(center);
            msgRed(player, lp);
        }
        startTask(session);
        player.openInventory(inventory);
        msg.send(player, red ? "opened-red" : "opened", "level", String.valueOf(level), "level-name", lp.name(),
                "time", String.valueOf(sessionSeconds));
    }

    private void msgRed(Player player, LockpickLevel lp) {
        plugin.messages().broadcast("red-announce", "player", player.getName(), "level", String.valueOf(lp.level()),
                "level-name", lp.name());
        FileConfiguration c = plugin.getConfig();
        String t = c.getString("red-shulker.title", "");
        String st = c.getString("red-shulker.subtitle", "");
        if (!t.isEmpty() || !st.isEmpty()) {
            Messages m = plugin.messages();
            player.showTitle(Title.title(
                    Text.parse(m.apply(t, "level", String.valueOf(lp.level()), "level-name", lp.name())),
                    Text.parse(m.apply(st, "level", String.valueOf(lp.level()), "level-name", lp.name()))));
        }
    }

    private void fill(Inventory inventory, List<ItemStack> items) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, ThreadLocalRandom.current());
        int n = Math.min(items.size(), slots.size());
        for (int i = 0; i < n; i++) {
            inventory.setItem(slots.get(i), items.get(i));
        }
    }

    // ------------------------------------------------------------------ Красный шалкер

    /** Удваивает лут: стакающиеся — x2 (с разбиением по стакам), нестакающиеся — дубликаты; исключения не трогает. */
    private List<ItemStack> applyRedBonus(List<ItemStack> loot) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : loot) {
            if (isExcluded(item)) {
                result.add(item);
                continue;
            }
            int max = Math.max(1, item.getMaxStackSize());
            if (max > 1) {
                int total = item.getAmount() * redMultiplier;
                while (total > 0) {
                    int n = Math.min(max, total);
                    ItemStack copy = item.clone();
                    copy.setAmount(n);
                    result.add(copy);
                    total -= n;
                }
            } else {
                for (int i = 0; i < redMultiplier; i++) {
                    result.add(item.clone());
                }
            }
        }
        return result;
    }

    private boolean isExcluded(ItemStack item) {
        Material m = item.getType();
        String n = m.name();
        if (exMaterials.contains(m)) {
            return true;
        }
        if (exArmor && (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || m == Material.ELYTRA || m == Material.SHIELD)) {
            return true;
        }
        if (exWeapons && (n.endsWith("_SWORD") || n.endsWith("_AXE") || m == Material.BOW || m == Material.CROSSBOW
                || m == Material.TRIDENT || m == Material.MACE)) {
            return true;
        }
        return exTools && (n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || m == Material.FISHING_ROD || m == Material.SHEARS || m == Material.FLINT_AND_STEEL);
    }

    // ------------------------------------------------------------------ таймер и завершение

    private void startTask(ShulkerSession session) {
        int[] counter = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (session.isClosed()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now >= session.expiresAt()) {
                finish(session, FinishReason.EXPIRED, false);
                return;
            }
            Location center = session.key().center();
            if (center != null) {
                (session.isRed() ? fxAmbientRed : fxAmbient).playParticles(center);
            }
            counter[0]++;
            if (actionbarTimer && counter[0] % 2 == 0) {
                Player owner = Bukkit.getPlayer(session.owner());
                if (owner != null) {
                    long left = (session.expiresAt() - now + 999) / 1000;
                    plugin.messages().actionBar(owner, "timer", "time", String.valueOf(left));
                }
            }
        }, 10L, 10L);
        session.setTask(task);
    }

    /**
     * Завершает сессию: остатки лута выпадают или удаляются, блок возвращается в исходное состояние,
     * зрители закрывают инвентарь. Повторный вызов безопасен (защита от дюпов).
     *
     * @param immediate true — закрыть окна сразу (onDisable), false — на следующем тике
     */
    public void finish(ShulkerSession session, FinishReason reason, boolean immediate) {
        if (session.isClosed()) {
            return;
        }
        session.markClosed();
        sessions.remove(session.key(), session);
        byOwner.remove(session.owner(), session);
        if (session.task() != null) {
            session.task().cancel();
        }

        // забираем содержимое и сразу очищаем инвентарь — ничто не может быть взято повторно
        Inventory inventory = session.inventory();
        ItemStack[] contents = inventory.getContents();
        inventory.clear();

        Block block = session.key().toBlock();
        Location center = session.key().center();
        int leftover = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            leftover++;
            if (dropLeftover && center != null && center.getWorld() != null) {
                World world = center.getWorld();
                world.dropItemNaturally(center.clone().add(0, 0.6, 0), item);
            }
        }

        if (block != null && Tag.SHULKER_BOXES.isTagged(block.getType())) {
            try {
                if (session.box() != null) {
                    session.box().close();
                }
            } catch (Exception ignored) {
                // блок мог быть заменён — крышка закроется сама
            }
            setMaterialKeepFacing(block, normalMaterial);
        }
        fxClose.play(center);

        Player owner = Bukkit.getPlayer(session.owner());
        if (owner != null && owner.isOnline()) {
            if (reason == FinishReason.EXPIRED) {
                plugin.messages().send(owner, "session-expired");
            } else if (reason == FinishReason.ADMIN) {
                plugin.messages().send(owner, "session-admin");
            }
            if (leftover > 0 && reason != FinishReason.SHUTDOWN && reason != FinishReason.QUIT) {
                plugin.messages().send(owner, dropLeftover ? "leftover-dropped" : "leftover-cleared", "count", String.valueOf(leftover));
            }
        }

        List<UUID> toClose = new ArrayList<>(session.viewers());
        session.viewers().clear();
        if (reason != FinishReason.OWNER_CLOSED && reason != FinishReason.QUIT) {
            toClose.add(session.owner());
        }
        Runnable closer = () -> {
            for (UUID id : toClose) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.getOpenInventory().getTopInventory().getHolder() == session.holder()) {
                    p.closeInventory();
                }
            }
        };
        if (immediate || !plugin.isEnabled()) {
            closer.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, closer);
        }
    }

    public ShulkerSession sessionOf(Block block) {
        return sessions.get(BlockKey.of(block));
    }

    public void onQuit(Player player) {
        denyCooldown.remove(player.getUniqueId());
        ShulkerSession own = byOwner.get(player.getUniqueId());
        if (own != null) {
            finish(own, FinishReason.QUIT, false);
        }
        for (ShulkerSession s : sessions.values()) {
            s.viewers().remove(player.getUniqueId());
        }
    }

    /** Завершает все сессии (выключение плагина, /mushulker reload). */
    public void shutdown() {
        for (ShulkerSession s : new ArrayList<>(sessions.values())) {
            finish(s, FinishReason.SHUTDOWN, true);
        }
        sessions.clear();
        byOwner.clear();
    }
}
