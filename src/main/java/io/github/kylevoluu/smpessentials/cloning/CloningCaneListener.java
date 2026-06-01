package io.github.kylevoluu.smpessentials.cloning;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.data.MobKills;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ModeStore;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Locale;

/** The Cloning Cane: confusion decoys / exploding clones; count scales with mob kills. */
public final class CloningCaneListener implements Listener {

    private static final String[] MODES = {"confusion", "explode"};

    private final Plugin plugin;
    private final AbilityManager abilities;

    public CloningCaneListener(Plugin plugin, AbilityManager abilities) {
        this.plugin = plugin;
        this.abilities = abilities;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(item, AmethystToolType.CLONING_CANE)) {
            return;
        }
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("cloning-cane.enabled", true)) {
            return;
        }

        if (player.isSneaking()) {
            String mode = ModeStore.cycle(item, Keys.CLONING_MODE, MODES);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(item);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
            player.updateInventory();
            player.sendActionBar(Component.text("Cloning Cane mode: " + mode.toUpperCase(Locale.ROOT)));
            return;
        }

        boolean explode = ModeStore.get(item, Keys.CLONING_MODE, MODES[0]).equals("explode");
        double cost = plugin.getConfig().getDouble("cloning-cane.energy-cost", 25);
        long cd = plugin.getConfig().getLong("cloning-cane.cooldown-ms", 3000);
        if (!abilities.tryUse(player, "cloning_cane", cost, cd)) {
            return;
        }

        int count = cloneCount(player);
        for (int i = 0; i < count; i++) {
            if (explode) {
                spawnExplodingClone(player);
            } else {
                spawnDecoy(player);
            }
        }
        ToolDamage.damageMainHand(player);
    }

    private int cloneCount(Player player) {
        int perClone = Math.max(1, plugin.getConfig().getInt("cloning-cane.kills-per-clone", 20));
        int max = Math.max(1, plugin.getConfig().getInt("cloning-cane.max-clones", 5));
        return Math.max(1, Math.min(max, 1 + MobKills.get(player) / perClone));
    }

    private ArmorStand baseClone(Player player) {
        Location loc = player.getLocation().add((Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);
        ArmorStand stand = player.getWorld().spawn(loc, ArmorStand.class);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.customName(player.name());
        stand.setCustomNameVisible(true);
        stand.getPersistentDataContainer().set(Keys.SUMMONED, PersistentDataType.BYTE, (byte) 1);
        EntityEquipment eq = stand.getEquipment();
        if (eq != null) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            if (armor.length == 4) {
                eq.setBoots(armor[0]);
                eq.setLeggings(armor[1]);
                eq.setChestplate(armor[2]);
                eq.setHelmet(armor[3]);
            }
        }
        return stand;
    }

    private void spawnDecoy(Player player) {
        ArmorStand stand = baseClone(player);
        stand.setGravity(false);
        int life = plugin.getConfig().getInt("cloning-cane.confusion.life-ticks", 80);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (stand.isValid()) {
                stand.getWorld().spawnParticle(Particle.CLOUD, stand.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
                stand.remove();
            }
        }, life);
    }

    private void spawnExplodingClone(Player player) {
        ArmorStand stand = baseClone(player);
        stand.setGravity(true);
        double power = plugin.getConfig().getDouble("cloning-cane.explode.power", 2.0);
        int maxTicks = plugin.getConfig().getInt("cloning-cane.explode.max-ticks", 120);
        World world = player.getWorld();

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!stand.isValid() || ticks >= maxTicks) {
                    if (stand.isValid()) {
                        stand.remove();
                    }
                    cancel();
                    return;
                }
                LivingEntity target = nearestEnemy(stand.getLocation(), player, 20);
                if (target != null) {
                    Vector dir = target.getLocation().toVector().subtract(stand.getLocation().toVector());
                    if (dir.lengthSquared() > 0.01) {
                        stand.setVelocity(dir.normalize().multiply(0.4));
                    }
                    if (stand.getLocation().distanceSquared(target.getLocation()) <= 4) {
                        world.createExplosion(stand.getLocation(), (float) power, false, false, player);
                        stand.remove();
                        cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private LivingEntity nearestEnemy(Location from, Player owner, double radius) {
        LivingEntity best = null;
        double bestSq = radius * radius;
        for (Entity e : from.getWorld().getNearbyEntities(from, radius, radius, radius)) {
            if (!(e instanceof LivingEntity living) || e.equals(owner)) {
                continue;
            }
            if (!(living instanceof Monster) && !(living instanceof Player)) {
                continue;
            }
            double sq = living.getLocation().distanceSquared(from);
            if (sq < bestSq) {
                bestSq = sq;
                best = living;
            }
        }
        return best;
    }
}
