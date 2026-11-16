package ru.miragestudio.sawervatoryy.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.miragestudio.sawervatoryy.MirageUShulkers;

import java.util.ArrayList;
import java.util.List;

/**
 * MirageUShulkers — сообщения из config.yml (секция messages).
 * Плейсхолдеры передаются парами: "player", "Steve", "level", "3".
 *
 * @author MirageStudio, sawervatoryy
 */
public final class Messages {

    private final MirageUShulkers plugin;

    public Messages(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    public List<String> rawList(String key) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "messages." + key;
        List<String> lines = new ArrayList<>();
        if (cfg.isList(path)) {
            lines.addAll(cfg.getStringList(path));
        } else if (cfg.isString(path)) {
            lines.add(cfg.getString(path, ""));
        } else {
            lines.add("<red>[MirageUShulkers] Нет сообщения: " + key);
        }
        return lines;
    }

    public String raw(String key) {
        return String.join("\n", rawList(key));
    }

    public String apply(String text, String... kv) {
        String result = text.replace("{prefix}", plugin.getConfig().getString("prefix", ""));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            result = result.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return result;
    }

    public Component get(String key, String... kv) {
        return Text.parse(apply(raw(key), kv));
    }

    public List<Component> getLines(String key, String... kv) {
        List<Component> result = new ArrayList<>();
        for (String line : rawList(key)) {
            result.add(Text.parse(apply(line, kv)));
        }
        return result;
    }

    public void send(CommandSender sender, String key, String... kv) {
        for (Component c : getLines(key, kv)) {
            sender.sendMessage(c);
        }
    }

    public void actionBar(Player player, String key, String... kv) {
        player.sendActionBar(get(key, kv));
    }

    public void broadcast(String key, String... kv) {
        for (Component c : getLines(key, kv)) {
            Bukkit.broadcast(c);
        }
    }
}
