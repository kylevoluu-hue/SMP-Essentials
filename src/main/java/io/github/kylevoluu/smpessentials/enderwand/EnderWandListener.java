package io.github.kylevoluu.smpessentials.enderwand;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ModeStore;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Locale;

/** The Ender Wand: modes pearls / ranged (wither+levitation) / summon (mini dragon). */
public final class EnderWandListener implements Listener {

    private static final String[] MODES = {"pearls", "ranged", "summon"};

    private final Plugin plugin;
    private final AbilityManager abilities;

    public EnderWandListener(Plugin plugin, AbilityManager abilities) {
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
        if (!AmethystToolFactory.is(item, AmethystToolType.ENDER_WAND)) {
            return;
        }
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("ender-wand.enabled", true)) {
            return;
        }

        if (player.isSneaking()) {
            String mode = ModeStore.cycle(item, Keys.ENDER_WAND_MODE, MODES);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(item);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
            player.updateInventory();
            player.sendActionBar(Component.text("Ender Wand mode: " + mode.toUpperCase(Locale.ROOT)));
            return;
        }

        switch (ModeStore.get(item, Keys.ENDER_WAND_MODE, MODES[0])) {
            case "ranged" -> ranged(player);
            case "summon" -> summon(player);
            default -> pearls(player);
        }
    }

    private void pearls(Player player) {
        double cost = plugin.getConfig().getDouble("ender-wand.pearls.energy-cost", 10);
        long cd = plugin.getConfig().getLong("ender-wand.pearls.cooldown-ms", 600);
        if (!abilities.tryUse(player, "ender_pearls", cost, cd)) {
            return;
        }
        player.launchProjectile(EnderPearl.class, player.getEyeLocation().getDirection());
        ToolDamage.damageMainHand(player);
    }

    private void ranged(Player player) {
        double cost = plugin.getConfig().getDouble("ender-wand.ranged.energy-cost", 20);
        long cd = plugin.getConfig().getLong("ender-wand.ranged.cooldown-ms", 1500);
        if (!abilities.tryUse(player, "ender_ranged", cost, cd)) {
            return;
        }
        Snowball shot = player.launchProjectile(Snowball.class, player.getEyeLocation().getDirection().multiply(1.5));
        shot.getPersistentDataContainer().set(Keys.ENDER_PROJECTILE, PersistentDataType.BYTE, (byte) 1);
        ToolDamage.damageMainHand(player);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(Keys.ENDER_PROJECTILE, PersistentDataType.BYTE)) {
            return;
        }
        int witherTicks = plugin.getConfig().getInt("ender-wand.ranged.wither-ticks", 140);
        int levitationTicks = plugin.getConfig().getInt("ender-wand.ranged.levitation-ticks", 60);
        int witherAmp = plugin.getConfig().getInt("ender-wand.ranged.wither-amplifier", 1);
        double radius = plugin.getConfig().getDouble("ender-wand.ranged.radius", 3.5);

        // AoE on impact so the target reliably gets BOTH wither and levitation,
        // even when the snowball lands a hair off the entity.
        Location at = event.getHitEntity() != null
                ? event.getHitEntity().getLocation()
                : event.getEntity().getLocation();
        for (Entity e : at.getWorld().getNearbyEntities(at, radius, radius, radius)) {
            if (e instanceof LivingEntity living) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, witherAmp));
                living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationTicks, 0));
            }
        }
        at.getWorld().spawn(at, AreaEffectCloud.class, cloud -> {
            cloud.setRadius((float) radius);
            cloud.setDuration(plugin.getConfig().getInt("ender-wand.ranged.cloud-ticks", 100));
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, witherAmp), true);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationTicks, 0), true);
        });
        at.getWorld().spawnParticle(Particle.PORTAL, at, 40, 0.6, 0.6, 0.6, 0.1);
    }

    private void summon(Player player) {
        if (!plugin.getConfig().getBoolean("ender-wand.summon.enabled", true)) {
            player.sendActionBar(Component.text("Dragon summon is disabled."));
            return;
        }
        double cost = plugin.getConfig().getDouble("ender-wand.summon.energy-cost", 50);
        long cd = plugin.getConfig().getLong("ender-wand.summon.cooldown-ms", 15000);
        if (!abilities.tryUse(player, "ender_summon", cost, cd)) {
            return;
        }
        int seconds = plugin.getConfig().getInt("ender-wand.summon.seconds", 6);
        double damage = plugin.getConfig().getDouble("ender-wand.summon.damage", 10);
        Vector aim = player.getEyeLocation().getDirection();
        Location spawn = player.getEyeLocation().add(aim.clone().multiply(4));
        EnderDragon dragon = player.getWorld().spawn(spawn, EnderDragon.class, d -> {
            d.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
            d.getPersistentDataContainer().set(Keys.SUMMONED, PersistentDataType.BYTE, (byte) 1);
        });

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= seconds * 20 || !dragon.isValid()) {
                    if (dragon.isValid()) {
                        dragon.remove();
                    }
                    cancel();
                    return;
                }
                dragon.setVelocity(aim.clone().multiply(1.2));
                for (Entity e : dragon.getWorld().getNearbyEntities(dragon.getLocation(), 4, 4, 4)) {
                    if (e instanceof LivingEntity living && !e.equals(dragon) && !e.equals(player)) {
                        living.setNoDamageTicks(0);
                        living.damage(damage, player);
                        living.setVelocity(aim.clone().multiply(1.5).setY(0.6));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        ToolDamage.damageMainHand(player);
    }
}
