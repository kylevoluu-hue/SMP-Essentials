package io.github.kylevoluu.smpessentials.sword;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PointedDripstone;
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
 * dripstone (tip pointing down) is summoned above it and falls; the bonus damage
 * is applied to the target as it lands, and the dripstone vanishes the instant it
 * touches a block (no block placed, no drop).
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
        double bonus = plugin.getConfig().getDouble("amethyst-tools.sword.dripstone-damage", 6.0);

        // Visual: a downward-pointing dripstone that falls onto the target.
        World world = victim.getWorld();
        Location spawn = victim.getLocation().add(0, height, 0);
        BlockData data = Material.POINTED_DRIPSTONE.createBlockData();
        if (data instanceof PointedDripstone dripstoneData) {
            dripstoneData.setVerticalDirection(BlockFace.DOWN);
            dripstoneData.setThickness(PointedDripstone.Thickness.TIP);
        }
        FallingBlock dripstone = world.spawnFallingBlock(spawn, data);
        dripstone.setDropItem(false);
        dripstone.setCancelDrop(true);
        dripstone.setHurtEntities(false);
        dripstone.getPersistentDataContainer().set(Keys.DRIPSTONE, PersistentDataType.BYTE, (byte) 1);

        // Damage the target as the dripstone reaches it. Resetting invulnerability
        // ticks ensures this bonus lands even right after the melee hit.
        if (bonus > 0) {
            long delay = Math.min(40, Math.max(6, height * 3L));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (victim.isValid() && !victim.isDead()) {
                    victim.setNoDamageTicks(0);
                    victim.damage(bonus);
                }
            }, delay);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock dripstone)) {
            return;
        }
        if (!dripstone.getPersistentDataContainer().has(Keys.DRIPSTONE, PersistentDataType.BYTE)) {
            return;
        }
        // Disappear immediately: never place a block, just remove the entity.
        event.setCancelled(true);
        dripstone.remove();
    }
}
