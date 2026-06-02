package io.github.kylevoluu.smpessentials.blaze;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.BlazeWand;
import io.github.kylevoluu.smpessentials.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Blaze Wand (blaze-rod design) with three sneak-switchable modes:
 * <ul>
 *   <li><b>summon</b> — spawns a temporary blaze that despawns after a few seconds;</li>
 *   <li><b>range</b> — fires a timed volley of fireballs (each dealing a fixed amount);</li>
 *   <li><b>bow</b> — lobs primed TNT that explodes and sets things on fire.</li>
 * </ul>
 */
public final class BlazeWandListener implements Listener {

    private final Plugin plugin;
    private final Messages messages;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public BlazeWandListener(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("blaze-wand.enabled", true);
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
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        ItemStack wand = hand == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(wand, AmethystToolType.BLAZE_WAND)) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (player.isSneaking()) {
            String mode = BlazeWand.cycleMode(wand);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(wand);
            } else {
                player.getInventory().setItemInMainHand(wand);
            }
            player.updateInventory();
            player.sendMessage(messages.prefixed("wand-mode-switched", "mode", mode.toUpperCase(Locale.ROOT)));
            return;
        }

        if (onCooldown(player)) {
            return;
        }
        switch (BlazeWand.getMode(wand)) {
            case BlazeWand.RANGE -> fireVolley(player);
            case BlazeWand.BOW -> lobTnt(player);
            default -> summonBlaze(player);
        }
    }

    private boolean onCooldown(Player player) {
        long cooldown = Math.max(0, plugin.getConfig().getLong("blaze-wand.cooldown-ms", 750));
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < cooldown) {
            return true;
        }
        lastUse.put(player.getUniqueId(), now);
        return false;
    }

    // --- summon -------------------------------------------------------------

    private void summonBlaze(Player player) {
        int seconds = Math.max(1, plugin.getConfig().getInt("blaze-wand.summon.seconds", 6));
        Location loc = targetLocation(player);
        World world = loc.getWorld();
        Blaze blaze = world.spawn(loc, Blaze.class);

        // If the player is looking at a living target, sic the blaze on it.
        RayTraceResult ray = player.rayTraceEntities(12);
        if (ray != null && ray.getHitEntity() instanceof LivingEntity target && !target.equals(player)) {
            blaze.setTarget(target);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (blaze.isValid()) {
                blaze.remove();
            }
        }, seconds * 20L);
    }

    private Location targetLocation(Player player) {
        RayTraceResult ray = player.rayTraceBlocks(8.0);
        if (ray != null && ray.getHitBlock() != null && ray.getHitBlockFace() != null) {
            Block place = ray.getHitBlock().getRelative(ray.getHitBlockFace());
            return place.getLocation().add(0.5, 0, 0.5);
        }
        Location eye = player.getEyeLocation();
        return eye.add(eye.getDirection().multiply(4));
    }

    // --- range --------------------------------------------------------------

    private void fireVolley(Player player) {
        int count = Math.max(1, plugin.getConfig().getInt("blaze-wand.range.fireballs", 5));
        long interval = Math.max(1, plugin.getConfig().getLong("blaze-wand.range.interval-ticks", 5));
        new BukkitRunnable() {
            int shot = 0;

            @Override
            public void run() {
                if (shot >= count || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                Vector direction = player.getEyeLocation().getDirection();
                SmallFireball fireball = player.launchProjectile(SmallFireball.class, direction);
                fireball.setIsIncendiary(false);
                fireball.getPersistentDataContainer().set(Keys.WAND_FIREBALL, PersistentDataType.BYTE, (byte) 1);
                shot++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof SmallFireball fireball)) {
            return;
        }
        if (!fireball.getPersistentDataContainer().has(Keys.WAND_FIREBALL, PersistentDataType.BYTE)) {
            return;
        }
        event.setDamage(plugin.getConfig().getDouble("blaze-wand.range.fireball-damage", 6.0));
    }

    // --- bow ----------------------------------------------------------------

    private void lobTnt(Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        TNTPrimed tnt = world.spawn(eye, TNTPrimed.class);
        tnt.setFuseTicks(Math.max(1, plugin.getConfig().getInt("blaze-wand.bow.fuse-ticks", 40)));
        double speed = plugin.getConfig().getDouble("blaze-wand.bow.speed", 1.6);
        tnt.setVelocity(eye.getDirection().multiply(speed));
        tnt.getPersistentDataContainer().set(Keys.WAND_TNT, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }
        if (!tnt.getPersistentDataContainer().has(Keys.WAND_TNT, PersistentDataType.BYTE)) {
            return;
        }
        int fireTicks = Math.max(0, plugin.getConfig().getInt("blaze-wand.bow.fire-ticks", 100));
        Location centre = tnt.getLocation();
        World world = centre.getWorld();

        // Ignite nearby living entities.
        for (Entity nearby : world.getNearbyEntities(centre, 5, 5, 5)) {
            if (nearby instanceof LivingEntity living) {
                living.setFireTicks(Math.max(living.getFireTicks(), fireTicks));
            }
        }
        // Place fire after the explosion clears the blocks (next tick).
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> scatterFire(world, centre), 1L);
    }

    private void scatterFire(World world, Location centre) {
        int placed = 0;
        for (int dx = -2; dx <= 2 && placed < 10; dx++) {
            for (int dz = -2; dz <= 2 && placed < 10; dz++) {
                for (int dy = -1; dy <= 2 && placed < 10; dy++) {
                    Block block = centre.clone().add(dx, dy, dz).getBlock();
                    if (block.getType().isAir() && block.getRelative(0, -1, 0).getType().isSolid()) {
                        block.setType(Material.FIRE, false);
                        placed++;
                    }
                }
            }
        }
    }
}
