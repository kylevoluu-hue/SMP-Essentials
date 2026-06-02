package io.github.kylevoluu.smpessentials.storm;

import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Storm Rod (lightning-rod design): right-click to call down a lightning bolt
 * wherever you are aiming, and smite anything you hit in melee.
 */
public final class StormRodListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public StormRodListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("storm-rod.enabled", true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!AmethystToolFactory.is(event.getItem(), AmethystToolType.STORM_ROD)) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);
        if (onCooldown(player)) {
            return;
        }

        double range = Math.max(1, plugin.getConfig().getDouble("storm-rod.range", 40));
        Location target = aimLocation(player, range);
        target.getWorld().strikeLightning(target);
        applyExtraDamage(target);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("storm-rod.melee-strike", true)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!AmethystToolFactory.is(attacker.getInventory().getItemInMainHand(), AmethystToolType.STORM_ROD)) {
            return;
        }
        victim.getWorld().strikeLightning(victim.getLocation());
    }

    private boolean onCooldown(Player player) {
        long cooldown = Math.max(0, plugin.getConfig().getLong("storm-rod.cooldown-ms", 1500));
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < cooldown) {
            return true;
        }
        lastUse.put(player.getUniqueId(), now);
        return false;
    }

    private Location aimLocation(Player player, double range) {
        RayTraceResult ray = player.rayTraceBlocks(range);
        if (ray != null && ray.getHitPosition() != null) {
            return ray.getHitPosition().toLocation(player.getWorld());
        }
        Location eye = player.getEyeLocation();
        return eye.add(eye.getDirection().multiply(range));
    }

    private void applyExtraDamage(Location strike) {
        double extra = plugin.getConfig().getDouble("storm-rod.extra-damage", 4.0);
        if (extra <= 0) {
            return;
        }
        for (Entity nearby : strike.getWorld().getNearbyEntities(strike, 3, 3, 3)) {
            if (nearby instanceof LivingEntity living) {
                living.damage(extra);
            }
        }
    }
}
