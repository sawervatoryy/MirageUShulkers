package ru.miragestudio.sawervatoryy.command;

import org.bukkit.block.Block;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.miragestudio.sawervatoryy.MirageUShulkers;
import ru.miragestudio.sawervatoryy.lockpick.LockpickLevel;
import ru.miragestudio.sawervatoryy.shulker.BlockKey;
import ru.miragestudio.sawervatoryy.util.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MirageUShulkers — команда /mushulker.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class MUShulkerCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("set", "remove", "bindkey", "editloot", "reload", "list", "help");
    private static final List<String> LEVELS = List.of("1", "2", "3", "4", "5");

    private final MirageUShulkers plugin;

    public MUShulkerCommand(MirageUShulkers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();
        if (!sender.hasPermission(MirageUShulkers.PERM_ADMIN)) {
            msg.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            msg.send(sender, "help");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleSet(sender);
            case "remove" -> handleRemove(sender);
            case "bindkey" -> handleBindKey(sender, args);
            case "editloot" -> handleEditLoot(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                msg.send(sender, "reload-ok");
            }
            case "list" -> handleList(sender);
            default -> msg.send(sender, "help");
        }
        return true;
    }

    private Block targetBlock(Player player) {
        return player.getTargetBlockExact(6);
    }

    private void handleSet(CommandSender sender) {
        Messages msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }
        Block block = targetBlock(player);
        if (block == null || block.getType().isAir()) {
            msg.send(sender, "no-target-block");
            return;
        }
        if (!Tag.SHULKER_BOXES.isTagged(block.getType())) {
            msg.send(sender, "set-not-shulker");
            return;
        }
        if (!plugin.shulkers().add(block)) {
            msg.send(sender, "set-already");
            return;
        }
        msg.send(sender, "set-ok", "world", block.getWorld().getName(), "x", String.valueOf(block.getX()),
                "y", String.valueOf(block.getY()), "z", String.valueOf(block.getZ()));
    }

    private void handleRemove(CommandSender sender) {
        Messages msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }
        Block block = targetBlock(player);
        if (block == null || block.getType().isAir()) {
            msg.send(sender, "no-target-block");
            return;
        }
        if (!plugin.shulkers().remove(block)) {
            msg.send(sender, "remove-not-found");
            return;
        }
        msg.send(sender, "remove-ok", "world", block.getWorld().getName(), "x", String.valueOf(block.getX()),
                "y", String.valueOf(block.getY()), "z", String.valueOf(block.getZ()));
    }

    private int parseLevel(String raw) {
        try {
            int lvl = Integer.parseInt(raw);
            return lvl >= 1 && lvl <= 5 ? lvl : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void handleBindKey(CommandSender sender, String[] args) {
        Messages msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "bindkey-usage");
            return;
        }
        int level = parseLevel(args[1]);
        if (level == 0) {
            msg.send(sender, "level-invalid");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            msg.send(sender, "bindkey-air");
            return;
        }
        int overlap = plugin.lockpicks().bind(level, hand);
        LockpickLevel lp = plugin.lockpicks().get(level);
        msg.send(sender, "bindkey-ok", "level", String.valueOf(level), "level-name", lp.name(), "item", hand.getType().name());
        if (hand.getItemMeta() == null || (!hand.getItemMeta().hasDisplayName() && !hand.getItemMeta().hasItemName()
                && hand.getItemMeta().getPersistentDataContainer().getKeys().isEmpty())) {
            msg.send(sender, "bindkey-plain");
        }
        if (overlap != 0) {
            msg.send(sender, "bindkey-overlap", "level", String.valueOf(level), "other", String.valueOf(overlap));
        }
    }

    private void handleEditLoot(CommandSender sender, String[] args) {
        Messages msg = plugin.messages();
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "editloot-usage");
            return;
        }
        int level = parseLevel(args[1]);
        if (level == 0) {
            msg.send(sender, "level-invalid");
            return;
        }
        plugin.editor().open(player, level);
    }

    private void handleList(CommandSender sender) {
        Messages msg = plugin.messages();
        List<BlockKey> all = plugin.shulkers().all();
        if (all.isEmpty()) {
            msg.send(sender, "list-empty");
            return;
        }
        msg.send(sender, "list-header", "count", String.valueOf(all.size()));
        int i = 1;
        for (BlockKey k : all) {
            String state = plugin.shulkers().isActive(k) ? "&#FF7B00открыт" : "&#00ACFFзакрыт";
            msg.send(sender, "list-line", "index", String.valueOf(i++), "world", k.world(), "x", String.valueOf(k.x()),
                    "y", String.valueOf(k.y()), "z", String.valueOf(k.z()), "state", state);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (!sender.hasPermission(MirageUShulkers.PERM_ADMIN)) {
            return result;
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : SUBS) {
                if (s.startsWith(prefix)) {
                    result.add(s);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("bindkey") || sub.equals("editloot")) {
                for (String l : LEVELS) {
                    if (l.startsWith(args[1])) {
                        result.add(l);
                    }
                }
            }
        }
        return result;
    }
}
