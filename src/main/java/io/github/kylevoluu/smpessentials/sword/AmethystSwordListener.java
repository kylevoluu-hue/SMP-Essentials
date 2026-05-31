package io.github.kylevoluu.smpessentials.sword;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * The Amethyst Sword's base damage (10) comes from an attribute modifier on the
 * item. This listener adds the flourish: on hitting a living entity, a pointed
 * dripstone is summoned above it, falls, deals bonus damage, and vanishes the
 * instant it lands (no block placed, no drop).
 */
public final class AmethystSwordListener implements Listener {

    private final Plugin plugin;

    public AmethystSwordListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("amethyst-tools.enabled", true)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(weapon, AmethystToolType.SWORD)) {
            return;
        }

        int height = Math.max(2, plugin.getConfig().getInt("amethyst-tools.sword.dripstone-height", 5));
        World world = victim.getWorld();
        Location spawn = victim.getLocation().add(0, height, 0);

        BlockData data = Material.POINTED_DRIPSTONE.createBlockData();
        FallingBlock dripstone = world.spawnFallingBlock(spawn, data);
        dripstone.setDropItem(false);
        dripstone.setCancelDrop(true);
        dripstone.setHurtEntities(false); // We apply the damage ourselves on landing.
        dripstone.getPersistentDataContainer().set(Keys.DRIPSTONE, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock dripstone)) {
            return;
        }
        if (!dripstone.getPersistentDataContainer().has(Keys.DRIPSTONE, PersistentDataType.BYTE)) {
            return;
        }
        // Disappear immediately: don't place the block, remove the entity.
        event.setCancelled(true);
        dripstone.remove();

        double damage = plugin.getConfig().getDouble("amethyst-tools.sword.dripstone-damage", 6.0);
        if (damage <= 0) {
            return;
        }
        Location at = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (Entity nearby : at.getWorld().getNearbyEntities(at, 1.2, 1.5, 1.2)) {
            if (nearby instanceof LivingEntity living && !isCreative(nearby)) {
                // No damage source -> fires EntityDamageEvent (not ByEntity), so this
                // never re-triggers the sword hit handler or combat tagging.
                living.damage(damage);
            }
        }
    }

    private boolean isCreative(Entity entity) {
        return entity instanceof Player player
                && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
    }
}
