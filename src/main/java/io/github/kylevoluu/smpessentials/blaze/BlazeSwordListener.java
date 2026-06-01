package io.github.kylevoluu.smpessentials.blaze;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * The Blaze Sword: base damage 12 (from an attribute modifier on the item), sets
 * whatever it hits on fire, and launches a small fireball at the target on each
 * hit. The fireball deals a configurable amount of extra damage (default 8) and
 * also ignites whatever it strikes.
 */
public final class BlazeSwordListener implements Listener {

    private final Plugin plugin;

    public BlazeSwordListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("blaze-sword.enabled", true);
    }

    private int fireTicks() {
        return Math.max(0, plugin.getConfig().getInt("blaze-sword.fire-ticks", 100));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!enabled()) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(weapon, AmethystToolType.BLAZE_SWORD)) {
            return;
        }

        // Light the target on fire.
        victim.setFireTicks(Math.max(victim.getFireTicks(), fireTicks()));

        // Shoot a fireball from the attacker toward the target.
        Vector direction = victim.getEyeLocation().toVector()
                .subtract(attacker.getEyeLocation().toVector());
        if (direction.lengthSquared() > 0) {
            direction.normalize();
        }
        SmallFireball fireball = attacker.launchProjectile(SmallFireball.class, direction);
        fireball.setIsIncendiary(plugin.getConfig().getBoolean("blaze-sword.incendiary", false));
        fireball.getPersistentDataContainer().set(Keys.BLAZE_FIREBALL, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof SmallFireball fireball)) {
            return;
        }
        if (!fireball.getPersistentDataContainer().has(Keys.BLAZE_FIREBALL, PersistentDataType.BYTE)) {
            return;
        }
        event.setDamage(plugin.getConfig().getDouble("blaze-sword.fireball-damage", 8.0));
        if (event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), fireTicks()));
        }
    }
}
