package io.github.kylevoluu.smpessentials.tools;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Plays the amethyst chime when a player selects an Amethyst tool on their
 * hotbar. The matching "mining" chime is played by the mining/tree-feller
 * listeners via {@link #playChime(Player)}.
 */
public final class ToolSoundListener implements Listener {

    private final Plugin plugin;

    public ToolSoundListener(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Whether the amethyst sound effects are enabled in config. */
    public static boolean enabled(Plugin plugin) {
        return plugin.getConfig().getBoolean("amethyst-tools.sound-effects", true);
    }

    /** Play the amethyst block chime at the player's location. */
    public static void playChime(Player player) {
        // Use the String-keyed overload: org.bukkit.Sound is registry-backed on
        // Paper 26.1, so a sound key is the stable, version-safe way to play it.
        player.playSound(player.getLocation(), "minecraft:block.amethyst_block.chime", 1.0f, 1.0f);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        if (!enabled(plugin)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (AmethystToolFactory.typeOf(newItem) != null) {
            playChime(player);
        }
    }
}
