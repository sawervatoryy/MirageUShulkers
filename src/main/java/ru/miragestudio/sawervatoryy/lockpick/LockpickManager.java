package ru.miragestudio.sawervatoryy.lockpick;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.util.ItemCodec;
import ru.miragestudio.sawervatoryy.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MirageUShulkers — определение уровня отмычки из MirageDungeons (1–5 LVL).
 * <p>
 * Порядок определения:
 * 1) сравнение с эталонными предметами, привязанными командой /mushulker bindkey;
 * 2) (опционально) поиск PDC-ключа пространства имён miragedungeons с уровнем 1–5.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class LockpickManager {

    public enum MatchMode {
        /** Полное совпадение метаданных (ItemStack#isSimilar). */
        STRICT,
        /** Материал + название + модель + все PDC-ключи эталона со значениями. */
        LOOSE,
        /** Материал + наличие PDC-ключей эталона (значения игнорируются). */
        KEYS
    }

    private static final Pattern TRAILING_DIGIT = Pattern.compile("(\\d)$");
    private static final Pattern ANY_DIGIT = Pattern.compile("(\\d)");

    private final MirageUShulkers plugin;
    private final LockpickLevel[] levels = new LockpickLevel[6];

    private MatchMode mode = MatchMode.LOOSE;
    private boolean pdcEnabled = true;
    private String pdcNamespace = "miragedungeons";
    private List<String> pdcHints = new ArrayList<>();

    public LockpickManager(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        try {
            mode = MatchMode.valueOf(cfg.getString("lockpicks.match-mode", "LOOSE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            mode = MatchMode.LOOSE;
        }
        pdcEnabled = cfg.getBoolean("lockpicks.pdc-detection.enabled", true);
        pdcNamespace = cfg.getString("lockpicks.pdc-detection.namespace", "miragedungeons").toLowerCase(Locale.ROOT);
        pdcHints = new ArrayList<>();
        for (String hint : cfg.getStringList("lockpicks.pdc-detection.key-hints")) {
            pdcHints.add(hint.toLowerCase(Locale.ROOT));
        }

        double[] defChance = {3, 6, 10, 15, 25};
        for (int lvl = 1; lvl <= 5; lvl++) {
            String base = "lockpicks.level-" + lvl;
            String encoded = cfg.getString(base + ".item", "");
            ItemStack reference = ItemCodec.decode(encoded);
            if (encoded != null && !encoded.isBlank() && reference == null) {
                plugin.getLogger().warning("Не удалось прочитать эталонную отмычку для " + base + " — перепривяжите: /mushulker bindkey " + lvl);
            }
            int min = Math.max(1, cfg.getInt(base + ".min-items", 2 + lvl));
            int max = Math.max(min, cfg.getInt(base + ".max-items", 4 + lvl));
            double chance = Math.max(0.0, Math.min(100.0, cfg.getDouble(base + ".red-chance", defChance[lvl - 1])));
            String name = cfg.getString(base + ".name", "&fОтмычка " + lvl + " ур.");
            levels[lvl] = new LockpickLevel(lvl, name, chance, min, max, reference);
        }
    }

    public LockpickLevel get(int level) {
        if (level < 1 || level > 5) {
            return null;
        }
        return levels[level];
    }

    public boolean isBound(int level) {
        LockpickLevel l = get(level);
        return l != null && l.reference() != null;
    }

    /**
     * Привязывает предмет в руке как эталон уровня.
     *
     * @return номер другого уровня, под которого этот предмет тоже подходит (0 — конфликтов нет)
     */
    public int bind(int level, ItemStack stack) {
        ItemStack single = stack.clone();
        single.setAmount(1);
        String base = "lockpicks.level-" + level;
        plugin.getConfig().set(base + ".item", ItemCodec.encode(single));
        plugin.getConfig().set(base + ".item-info", describe(single));
        plugin.saveConfig();
        load();
        for (int other = 1; other <= 5; other++) {
            if (other != level && levels[other].reference() != null && matches(levels[other].reference(), single)) {
                return other;
            }
        }
        return 0;
    }

    private String describe(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        String name = "";
        if (meta != null && meta.displayName() != null) {
            name = Text.plain(meta.displayName());
        } else if (meta != null && meta.hasItemName()) {
            name = Text.plain(meta.itemName());
        }
        return stack.getType().name() + (name.isEmpty() ? "" : " | " + name);
    }

    /** @return уровень 1..5 либо 0, если предмет не является отмычкой. */
    public int detect(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return 0;
        }
        for (int lvl = 5; lvl >= 1; lvl--) {
            ItemStack ref = levels[lvl] == null ? null : levels[lvl].reference();
            if (ref != null && matches(ref, stack)) {
                return lvl;
            }
        }
        if (pdcEnabled) {
            return detectByPdc(stack);
        }
        return 0;
    }

    // ------------------------------------------------------------------ matching

    private boolean matches(ItemStack ref, ItemStack candidate) {
        if (ref.getType() != candidate.getType()) {
            return false;
        }
        switch (mode) {
            case STRICT:
                return ref.isSimilar(candidate);
            case KEYS: {
                ItemMeta rm = ref.getItemMeta();
                ItemMeta cm = candidate.getItemMeta();
                if (rm == null) {
                    return true;
                }
                if (cm == null) {
                    return false;
                }
                if (rm.getPersistentDataContainer().getKeys().isEmpty()) {
                    return looseMatch(rm, cm);
                }
                return pdcKeysMatch(rm.getPersistentDataContainer(), cm.getPersistentDataContainer(), false);
            }
            case LOOSE:
            default: {
                ItemMeta rm = ref.getItemMeta();
                ItemMeta cm = candidate.getItemMeta();
                if (rm == null) {
                    return true;
                }
                if (cm == null) {
                    return false;
                }
                return looseMatch(rm, cm);
            }
        }
    }

    private boolean looseMatch(ItemMeta rm, ItemMeta cm) {
        if (!Objects.equals(plainOrEmpty(rm.displayName()), plainOrEmpty(cm.displayName()))) {
            return false;
        }
        Component rItem = rm.hasItemName() ? rm.itemName() : null;
        Component cItem = cm.hasItemName() ? cm.itemName() : null;
        if (!Objects.equals(plainOrEmpty(rItem), plainOrEmpty(cItem))) {
            return false;
        }
        if (rm.hasItemModel() != cm.hasItemModel() || !Objects.equals(rm.getItemModel(), cm.getItemModel())) {
            return false;
        }
        if (rm.hasCustomModelDataComponent() != cm.hasCustomModelDataComponent()) {
            return false;
        }
        if (rm.hasCustomModelDataComponent()) {
            var rc = rm.getCustomModelDataComponent();
            var cc = cm.getCustomModelDataComponent();
            if (!rc.getFloats().equals(cc.getFloats()) || !rc.getStrings().equals(cc.getStrings())) {
                return false;
            }
        }
        return pdcKeysMatch(rm.getPersistentDataContainer(), cm.getPersistentDataContainer(), true);
    }

    private String plainOrEmpty(Component c) {
        return c == null ? "" : Text.plain(c);
    }

    private boolean pdcKeysMatch(PersistentDataContainer ref, PersistentDataContainer cand, boolean compareValues) {
        for (NamespacedKey key : ref.getKeys()) {
            if (!cand.getKeys().contains(key)) {
                return false;
            }
            if (compareValues) {
                Object a = readAny(ref, key);
                Object b = readAny(cand, key);
                if (a != null && !Objects.deepEquals(a, b)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Object readAny(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.STRING)) {
            return pdc.get(key, PersistentDataType.STRING);
        }
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return pdc.get(key, PersistentDataType.INTEGER);
        }
        if (pdc.has(key, PersistentDataType.LONG)) {
            return pdc.get(key, PersistentDataType.LONG);
        }
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            return pdc.get(key, PersistentDataType.DOUBLE);
        }
        if (pdc.has(key, PersistentDataType.FLOAT)) {
            return pdc.get(key, PersistentDataType.FLOAT);
        }
        if (pdc.has(key, PersistentDataType.SHORT)) {
            return pdc.get(key, PersistentDataType.SHORT);
        }
        if (pdc.has(key, PersistentDataType.BYTE)) {
            return pdc.get(key, PersistentDataType.BYTE);
        }
        if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
            return pdc.get(key, PersistentDataType.BYTE_ARRAY);
        }
        if (pdc.has(key, PersistentDataType.INTEGER_ARRAY)) {
            return pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        }
        if (pdc.has(key, PersistentDataType.LONG_ARRAY)) {
            return pdc.get(key, PersistentDataType.LONG_ARRAY);
        }
        return null;
    }

    // ------------------------------------------------------------------ PDC fallback

    private int detectByPdc(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equalsIgnoreCase(pdcNamespace)) {
                continue;
            }
            String name = key.getKey().toLowerCase(Locale.ROOT);
            boolean hinted = pdcHints.isEmpty();
            for (String hint : pdcHints) {
                if (name.contains(hint)) {
                    hinted = true;
                    break;
                }
            }
            if (!hinted) {
                continue;
            }
            int fromValue = levelFromValue(pdc, key);
            if (fromValue >= 1 && fromValue <= 5) {
                return fromValue;
            }
            Matcher m = TRAILING_DIGIT.matcher(name);
            if (m.find()) {
                int lvl = Integer.parseInt(m.group(1));
                if (lvl >= 1 && lvl <= 5) {
                    return lvl;
                }
            }
        }
        return 0;
    }

    private int levelFromValue(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            Integer v = pdc.get(key, PersistentDataType.INTEGER);
            return v == null ? 0 : v;
        }
        if (pdc.has(key, PersistentDataType.LONG)) {
            Long v = pdc.get(key, PersistentDataType.LONG);
            return v == null ? 0 : v.intValue();
        }
        if (pdc.has(key, PersistentDataType.BYTE)) {
            Byte v = pdc.get(key, PersistentDataType.BYTE);
            return v == null ? 0 : v;
        }
        if (pdc.has(key, PersistentDataType.SHORT)) {
            Short v = pdc.get(key, PersistentDataType.SHORT);
            return v == null ? 0 : v;
        }
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            Double v = pdc.get(key, PersistentDataType.DOUBLE);
            return v == null ? 0 : v.intValue();
        }
        if (pdc.has(key, PersistentDataType.STRING)) {
            String v = pdc.get(key, PersistentDataType.STRING);
            if (v != null) {
                Matcher m = ANY_DIGIT.matcher(v);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return 0;
    }
}
