package io.github.kylevoluu.smpessentials.wardencrossbow;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * The Warden Crossbow: right-click fires a sonic-boom beam (raycast damage +
 * particles + sound). With Multishot it fires three booms in a spread.
 */
public final class WardenCrossbowListener implements Listener {

    private final Plugin plugin;
    private final AbilityManager abilities;

    public WardenCrossbowListener(Plugin plugin, AbilityManager abilities) {
        this.plugin = plugin;
        this.abilities = abilities;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!AmethystToolFactory.is(event.getItem(), AmethystToolType.WARDEN_CROSSBOW)) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!plugin.getConfig().getBoolean("warden-crossbow.enabled", true)) {
            return;
        }
        double cost = plugin.getConfig().getDouble("warden-crossbow.energy-cost", 25);
        long cooldown = plugin.getConfig().getLong("warden-crossbow.cooldown-ms", 1500);
        if (!abilities.tryUse(player, "warden_crossbow", cost, cooldown)) {
            return;
        }

        boolean multishot = hasMultishot(event.getItem());
        Vector base = player.getEyeLocation().getDirection();
        if (multishot) {
            fire(player, rotateYaw(base, -12));
            fire(player, base);
            fire(player, rotateYaw(base, 12));
        } else {
            fire(player, base);
        }
        ToolDamage.damageMainHand(player);
    }

    private void fire(Player shooter, Vector direction) {
        double range = plugin.getConfig().getDouble("warden-crossbow.range", 24);
        double damage = plugin.getConfig().getDouble("warden-crossbow.damage", 10);
        double knockback = plugin.getConfig().getDouble("warden-crossbow.knockback", 1.2);
        World world = shooter.getWorld();
        Location start = shooter.getEyeLocation();
        Vector step = direction.clone().normalize().multiply(0.75);
        Location point = start.clone();
        Set<Entity> hit = new HashSet<>();

        world.playSound(start, "minecraft:entity.warden.sonic_boom", 1.5f, 1.0f);
        for (double travelled = 0; travelled <= range; travelled += 0.75) {
            point.add(step);
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            for (Entity entity : world.getNearbyEntities(point, 1.4, 1.4, 1.4)) {
                if (entity.equals(shooter) || !(entity instanceof LivingEntity living) || !hit.add(entity)) {
                    continue;
                }
                living.damage(damage, shooter);
                living.setVelocity(living.getVelocity().add(step.clone().normalize().multiply(knockback)));
            }
        }
    }

    private boolean hasMultishot(ItemStack item) {
        Enchantment multishot = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft("multishot"));
        return multishot != null && item.getEnchantmentLevel(multishot) > 0;
    }

    private Vector rotateYaw(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }
}
