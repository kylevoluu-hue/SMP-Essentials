package io.github.kylevoluu.smpessentials.command;

import io.github.kylevoluu.smpessentials.SmpEssentials;
import io.github.kylevoluu.smpessentials.combatlog.CombatTagManager;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Handles {@code /smpe give|combat|reload} (alias {@code /smpessentials}). */
public final class SmpCommand implements CommandExecutor, TabCompleter {

    private final SmpEssentials plugin;
    private final CombatTagManager combat;
    private final Messages messages;

    public SmpCommand(SmpEssentials plugin, CombatTagManager combat, Messages messages) {
        this.plugin = plugin;
        this.combat = combat;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.prefixed("combat-status-self-none"));
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "combat" -> handleCombat(sender, args);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smpessentials.give")) {
            sender.sendMessage(messages.prefixed("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("combat-status-self-none"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("Usage: /smpe give <pickaxe|axe> [player]"));
            return;
        }
        AmethystToolType type = AmethystToolType.fromArgument(args[1]);
        if (type == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("Unknown tool. Use 'pickaxe' or 'axe'."));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("Player '" + args[2] + "' is not online."));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(net.kyori.adventure.text.Component.text("Console must specify a player."));
            return;
        }

        ItemStack tool = AmethystToolFactory.create(type);
        target.getInventory().addItem(tool).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        target.sendMessage(messages.prefixed("tool-received", "tool", type.displayName()));
        if (!target.equals(sender)) {
            sender.sendMessage(messages.prefixed("tool-given", "tool", type.displayName(), "player", target.getName()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("smpessentials.reload")) {
            sender.sendMessage(messages.prefixed("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(messages.prefixed("reload-success"));
    }

    private void handleCombat(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("Player '" + args[1] + "' is not online."));
                return;
            }
            if (combat.isTagged(target.getUniqueId())) {
                sender.sendMessage(messages.prefixed("combat-status-other",
                        "player", target.getName(), "seconds", String.valueOf(combat.remainingSeconds(target.getUniqueId()))));
            } else {
                sender.sendMessage(messages.prefixed("combat-status-other-none", "player", target.getName()));
            }
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("Console is never in combat."));
            return;
        }
        if (combat.isTagged(player.getUniqueId())) {
            sender.sendMessage(messages.prefixed("combat-status-self",
                    "seconds", String.valueOf(combat.remainingSeconds(player.getUniqueId()))));
        } else {
            sender.sendMessage(messages.prefixed("combat-status-self-none"));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("/" + label + " give <pickaxe|axe> [player]"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/" + label + " combat [player]"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/" + label + " reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("give", "combat", "reload")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String tool : List.of("pickaxe", "axe")) {
                if (tool.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(tool);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")
                || args.length == 2 && args[0].equalsIgnoreCase("combat")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                out.add(player.getName());
            }
        }
        return out;
    }
}
