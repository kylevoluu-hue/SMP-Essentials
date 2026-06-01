package io.github.kylevoluu.smpessentials.troll;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The Troll Staff: right-click to play a random eerie sound to nearby players
 * (a harmless jump-scare).
 */
public final class TrollStaffListener implements Listener {

    private static final String[] SOUNDS = {
            "minecraft:entity.warden.emerge",
            "minecraft:ambient.cave",
            "minecraft:entity.creeper.primed",
            "minecraft:entity.generic.explode",
            "minecraft:block.stone.break",
            "minecraft:block.stone.place",
            "minecraft:entity.player.hurt",
            "minecraft:block.sculk_shrieker.shriek",
            "minecraft:entity.warden.nearby_closer"
    };

    private final Plugin plugin;
    private final AbilityManager abilities;

    public TrollStaffListener(Plugin plugin, AbilityManager abilities) {
        this.plugin = plugin;
        this.abilities = abilities;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!AmethystToolFactory.is(event.getItem(), AmethystToolType.TROLL_STAFF)) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);

        double cost = plugin.getConfig().getDouble("troll-staff.energy-cost", 15);
        long cooldown = plugin.getConfig().getLong("troll-staff.cooldown-ms", 1000);
        if (!abilities.tryUse(player, "troll", cost, cooldown)) {
            return;
        }

        String sound = SOUNDS[ThreadLocalRandom.current().nextInt(SOUNDS.length)];
        double radius = plugin.getConfig().getDouble("troll-staff.radius", 24);
        double radiusSq = radius * radius;
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                nearby.playSound(nearby.getLocation(), sound, 1.0f, 1.0f);
            }
        }
        ToolDamage.damageMainHand(player);
    }
}
