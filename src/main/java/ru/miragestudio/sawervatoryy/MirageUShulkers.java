package ru.miragestudio.sawervatoryy;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.miragestudio.sawervatoryy.command.MUShulkerCommand;
import ru.miragestudio.sawervatoryy.gui.LootEditor;
import ru.miragestudio.sawervatoryy.listener.EditorListener;
import ru.miragestudio.sawervatoryy.listener.ProtectionListener;
import ru.miragestudio.sawervatoryy.listener.ShulkerListener;
import ru.miragestudio.sawervatoryy.lockpick.LockpickManager;
import ru.miragestudio.sawervatoryy.loot.LootManager;
import ru.miragestudio.sawervatoryy.shulker.ShulkerManager;
import ru.miragestudio.sawervatoryy.util.Messages;
import ru.miragestudio.sawervatoryy.util.Text;

import java.io.File;

/**
 * MirageUShulkers — Шалкер сокровищ с привязкой к MirageDungeons (отмычки 1–5 LVL).
 *
 * @author MirageStudio, sawervatoryy
 */
public final class MirageUShulkers extends JavaPlugin {

    public static final String PERM_ADMIN = "mirageushulkers.admin";
    public static final String PERM_USE = "mirageushulkers.use";

    private Messages messages;
    private LockpickManager lockpicks;
    private LootManager loot;
    private ShulkerManager shulkers;
    private LootEditor editor;
    private boolean dungeonsLinked;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "loot.yml").exists()) {
            saveResource("loot.yml", false);
        }

        messages = new Messages(this);
        lockpicks = new LockpickManager(this);
        loot = new LootManager(this);
        shulkers = new ShulkerManager(this);
        editor = new LootEditor(this);

        lockpicks.load();
        loot.load();
        shulkers.reload();

        getServer().getPluginManager().registerEvents(new ShulkerListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(this), this);

        MUShulkerCommand executor = new MUShulkerCommand(this);
        PluginCommand command = getCommand("mushulker");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        Plugin dungeons = getServer().getPluginManager().getPlugin("MirageDungeons");
        dungeonsLinked = dungeons != null && dungeons.isEnabled();
        printBanner();

        Bukkit.getScheduler().runTask(this, () -> shulkers.normalizeLoaded());
    }

    @Override
    public void onDisable() {
        // порядок важен: сначала закрываем сессии (остатки лута возвращаются в мир), затем редактор
        if (shulkers != null) {
            shulkers.shutdown();
        }
        if (editor != null) {
            editor.shutdown();
        }
        if (loot != null) {
            loot.saveIfDirty();
        }
    }

    /** Перезагрузка config.yml и loot.yml. Активные сессии завершаются безопасно. */
    public void reloadAll() {
        shulkers.shutdown();
        editor.shutdown();
        reloadConfig();
        lockpicks.load();
        loot.load();
        shulkers.reload();
        Bukkit.getScheduler().runTask(this, () -> shulkers.normalizeLoaded());
    }

    private void printBanner() {
        CommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(Text.parse(""));
        console.sendMessage(Text.parse("<gradient:#00ACFF:#00B0F2><bold>MirageStudio</bold></gradient> &8| &fMirageUShulkers &7v" + getPluginMeta().getVersion()));
        console.sendMessage(Text.parse("&8  ▸ &7Авторы: &#00ACFFMirageStudio&7, &#00ACFFsawervatoryy"));
        if (dungeonsLinked) {
            console.sendMessage(Text.parse("&8  ▸ &a✔ &#00ACFFMirageStudio&f: &bMirageUShulkers &8<-> &bMirageDungeons &7— модули успешно связаны"));
        } else {
            console.sendMessage(Text.parse("&8  ▸ &e⚠ &7MirageDungeons не найден — отмычки определяются по привязанным эталонам (&f/mushulker bindkey <1-5>&7)"));
        }
        console.sendMessage(Text.parse(""));
    }

    public Messages messages() {
        return messages;
    }

    public LockpickManager lockpicks() {
        return lockpicks;
    }

    public LootManager loot() {
        return loot;
    }

    public ShulkerManager shulkers() {
        return shulkers;
    }

    public LootEditor editor() {
        return editor;
    }

    public boolean isDungeonsLinked() {
        return dungeonsLinked;
    }
}
