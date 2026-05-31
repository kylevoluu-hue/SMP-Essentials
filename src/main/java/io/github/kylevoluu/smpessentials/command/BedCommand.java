package io.github.kylevoluu.smpessentials.command;

import io.github.kylevoluu.smpessentials.util.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /bed} — teleport the player to their bed/respawn point, or tell them to
 * set one if they have none. While in combat the command is blocked by the
 * anti-combat-log system (via {@code combat.blocked-commands}).
 */
public final class BedCommand implements CommandExecutor {

    private final Messages messages;

    public BedCommand(Messages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /bed."));
            return true;
        }
        Location bed = player.getRespawnLocation();
        if (bed == null) {
            player.sendMessage(messages.prefixed("bed-none"));
            return true;
        }
        player.teleport(bed);
        player.sendMessage(messages.prefixed("bed-teleported"));
        return true;
    }
}
