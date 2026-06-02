package io.github.kylevoluu.smpessentials.troll;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Troll Staff: right-click plays a random eerie sound to nearby players;
 * sneak + right-click spawns a "Herobrine" (a Herobrine-skinned head) in view of
 * or behind the player that vanishes the instant they look at it.
 */
public final class TrollStaffListener implements Listener {

    private static final String[] SOUNDS = {
            "minecraft:ambient.cave",
            "minecraft:entity.warden.emerge",
            "minecraft:entity.warden.nearby_closer",
            "minecraft:entity.warden.heartbeat",
            "minecraft:entity.warden.listening_angry",
            "minecraft:entity.creeper.primed",
            "minecraft:entity.generic.explode",
            "minecraft:entity.enderman.scream",
            "minecraft:entity.enderman.stare",
            "minecraft:entity.ghast.scream",
            "minecraft:entity.wither.spawn",
            "minecraft:entity.elder_guardian.curse",
            "minecraft:block.sculk_shrieker.shriek",
            "minecraft:ambient.soul_sand_valley.mood",
            "minecraft:ambient.basalt_deltas.mood",
            "minecraft:ambient.crimson_forest.mood",
            "minecraft:entity.skeleton.step",
            "minecraft:block.deepslate.break"
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

        if (player.isSneaking()) {
            spawnHerobrine(player);
        } else {
            scare(player);
        }
    }

    private void scare(Player player) {
        double cost = plugin.getConfig().getDouble("troll-staff.energy-cost", 15);
        long cooldown = plugin.getConfig().getLong("troll-staff.cooldown-ms", 1000);
        if (!abilities.tryUse(player, "troll", cost, cooldown)) {
            return;
        }
        String sound = SOUNDS[ThreadLocalRandom.current().nextInt(SOUNDS.length)];
        float pitch = 0.6f + ThreadLocalRandom.current().nextFloat() * 0.5f;
        double radius = plugin.getConfig().getDouble("troll-staff.radius", 24);
        double radiusSq = radius * radius;
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                nearby.playSound(nearby.getLocation(), sound, 1.0f, pitch);
            }
        }
        ToolDamage.damageMainHand(player);
    }

    private void spawnHerobrine(Player player) {
        double cost = plugin.getConfig().getDouble("troll-staff.herobrine.energy-cost", 20);
        long cooldown = plugin.getConfig().getLong("troll-staff.herobrine.cooldown-ms", 8000);
        if (!abilities.tryUse(player, "troll_herobrine", cost, cooldown)) {
            return;
        }
        double distance = plugin.getConfig().getDouble("troll-staff.herobrine.distance", 6);
        boolean behind = Math.random() < plugin.getConfig().getDouble("troll-staff.herobrine.behind-chance", 0.5);
        int timeout = plugin.getConfig().getInt("troll-staff.herobrine.timeout-ticks", 200);

        Vector dir = player.getEyeLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) {
            dir = new Vector(0, 0, 1);
        }
        dir = dir.normalize().multiply(behind ? -distance : distance);
        Location spawn = player.getLocation().add(dir);
        spawn.setYaw(yawToward(spawn, player.getLocation()));
        spawn.setPitch(0);

        ArmorStand hero = player.getWorld().spawn(spawn, ArmorStand.class, s -> {
            s.setVisible(false);     // only the head shows -> a floating Herobrine head
            s.setBasePlate(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setCanPickupItems(false);
            s.getPersistentDataContainer().set(Keys.SUMMONED, PersistentDataType.BYTE, (byte) 1);
            EntityEquipment eq = s.getEquipment();
            if (eq != null) {
                eq.setHelmet(herobrineHead());
            }
        });

        player.playSound(player.getLocation(), "minecraft:ambient.cave", 1.0f, 0.55f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!hero.isValid()) {
                    cancel();
                    return;
                }
                if (ticks >= timeout || !player.isOnline() || isSeen(player, hero)) {
                    hero.getWorld().spawnParticle(Particle.CLOUD, hero.getLocation().add(0, 1.6, 0),
                            12, 0.2, 0.3, 0.2, 0.01);
                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), "minecraft:entity.enderman.teleport", 0.8f, 0.6f);
                    }
                    hero.remove();
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 8L, 4L); // brief delay so a front spawn is glimpsed before vanishing
        ToolDamage.damageMainHand(player);
    }

    /** True when the player is looking roughly at, and has line of sight to, the entity. */
    private boolean isSeen(Player player, Entity entity) {
        Vector to = entity.getLocation().add(0, 1.6, 0).toVector().subtract(player.getEyeLocation().toVector());
        if (to.lengthSquared() < 0.01) {
            return true;
        }
        double dot = player.getEyeLocation().getDirection().normalize().dot(to.normalize());
        return dot > 0.55 && player.hasLineOfSight(entity);
    }

    private ItemStack herobrineHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        String url = plugin.getConfig().getString("troll-staff.herobrine.skin-url", "");
        if (url != null && !url.isBlank() && head.getItemMeta() instanceof SkullMeta meta) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "Herobrine");
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(java.net.URI.create(url).toURL());
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
                head.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid Herobrine skin-url: " + e.getMessage());
            }
        }
        return head;
    }

    private float yawToward(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
