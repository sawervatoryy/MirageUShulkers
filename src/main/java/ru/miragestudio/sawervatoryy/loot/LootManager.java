package ru.miragestudio.sawervatoryy.loot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.util.ItemCodec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MirageUShulkers — хранение и генерация лута (loot.yml, level-1..level-5).
 * Предметы хранятся в Base64 (нативная сериализация ItemStack со всеми Data Components 1.21.4);
 * для ручного редактирования допускается поле material.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class LootManager {

    private final MirageUShulkers plugin;
    private final File file;
    private final Map<Integer, List<LootEntry>> entries = new HashMap<>();
    private boolean dirty;

    public LootManager(MirageUShulkers plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "loot.yml");
        for (int i = 1; i <= 5; i++) {
            entries.put(i, new ArrayList<>());
        }
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("loot.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (int lvl = 1; lvl <= 5; lvl++) {
            List<LootEntry> list = new ArrayList<>();
            ConfigurationSection sec = yaml.getConfigurationSection("level-" + lvl);
            if (sec != null) {
                for (String id : sec.getKeys(false)) {
                    ConfigurationSection e = sec.getConfigurationSection(id);
                    if (e == null) {
                        continue;
                    }
                    ItemStack item = ItemCodec.decode(e.getString("item", ""));
                    if (item == null) {
                        Material mat = Material.matchMaterial(e.getString("material", ""));
                        if (mat != null && !mat.isAir() && mat.isItem()) {
                            item = new ItemStack(mat);
                        }
                    }
                    if (item == null) {
                        plugin.getLogger().warning("loot.yml: level-" + lvl + "." + id + " — предмет не прочитан, запись пропущена.");
                        continue;
                    }
                    list.add(new LootEntry(item, e.getDouble("chance", 10.0), e.getInt("min-amount", 1), e.getInt("max-amount", 1)));
                }
            }
            entries.put(lvl, list);
        }
        dirty = false;
    }

    /** Живой список записей уровня (его правит редактор лута). */
    public List<LootEntry> entries(int level) {
        return entries.computeIfAbsent(level, k -> new ArrayList<>());
    }

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        out.options().setHeader(List.of(
                "MirageUShulkers — loot.yml (MirageStudio / sawervatoryy)",
                "Лут разделён на 5 уровней отмычек: level-1 ... level-5.",
                "Удобнее всего редактировать через /mushulker editloot <1-5>.",
                "Поле item — Base64 нативной сериализации ItemStack (все Data Components сохраняются).",
                "Вручную можно писать записи с полем material (например DIAMOND) вместо item."));
        for (int lvl = 1; lvl <= 5; lvl++) {
            ConfigurationSection sec = out.createSection("level-" + lvl);
            int i = 1;
            for (LootEntry e : entries(lvl)) {
                ConfigurationSection c = sec.createSection(String.valueOf(i++));
                c.set("material", e.item().getType().name());
                c.set("chance", e.chance());
                c.set("min-amount", e.minAmount());
                c.set("max-amount", e.maxAmount());
                c.set("item", ItemCodec.encode(e.item()));
            }
        }
        try {
            out.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить loot.yml: " + ex.getMessage());
        }
    }

    /**
     * Генерирует лут уровня: каждая запись проходит бросок по своему шансу, число стаков — в диапазоне [minItems; maxItems].
     */
    public List<ItemStack> generate(int level, int minItems, int maxItems, int maxSlots) {
        List<LootEntry> pool = new ArrayList<>();
        for (LootEntry e : entries(level)) {
            if (e.chance() > 0) {
                pool.add(e);
            }
        }
        List<ItemStack> result = new ArrayList<>();
        if (pool.isEmpty()) {
            return result;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int hi = Math.max(1, Math.min(Math.max(minItems, maxItems), maxSlots));
        int lo = Math.max(1, Math.min(minItems, hi));
        int target = lo >= hi ? hi : rnd.nextInt(lo, hi + 1);

        List<LootEntry> remaining = new ArrayList<>(pool);
        Collections.shuffle(remaining, rnd);
        List<LootEntry> picked = new ArrayList<>();

        for (LootEntry e : new ArrayList<>(remaining)) {
            if (picked.size() >= target) {
                break;
            }
            if (rnd.nextDouble() * 100.0 < e.chance()) {
                picked.add(e);
                remaining.remove(e);
            }
        }
        while (picked.size() < lo && !remaining.isEmpty()) {
            double total = 0;
            for (LootEntry e : remaining) {
                total += e.chance();
            }
            double roll = rnd.nextDouble() * total;
            LootEntry chosen = remaining.get(remaining.size() - 1);
            for (LootEntry e : remaining) {
                roll -= e.chance();
                if (roll <= 0) {
                    chosen = e;
                    break;
                }
            }
            picked.add(chosen);
            remaining.remove(chosen);
        }

        for (LootEntry e : picked) {
            int amount = e.minAmount() >= e.maxAmount() ? e.minAmount() : rnd.nextInt(e.minAmount(), e.maxAmount() + 1);
            ItemStack stack = e.item().clone();
            stack.setAmount(Math.max(1, Math.min(amount, Math.max(1, stack.getMaxStackSize()))));
            result.add(stack);
        }
        return result;
    }
}
