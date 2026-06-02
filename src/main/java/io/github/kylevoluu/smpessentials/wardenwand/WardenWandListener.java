package io.github.kylevoluu.smpessentials.wardenwand;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ModeStore;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The Warden Wand: modes summon (attacking bats) / range (bat jumpscare) / boss (Warden Form). */
public final class WardenWandListener implements Listener {

    private static final String[] MODES = {"summon", "range", "boss"};

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Map<UUID, ItemStack[]> bossInventories = new HashMap<>();
    private final Map<UUID, AttributeModifier> bossHpModifier = new HashMap<>();
    private final Map<UUID, AttributeModifier> bossScaleModifier = new HashMap<>();
    private final Set<UUID> bossActive = new HashSet<>();

    public WardenWandListener(Plugin plugin, AbilityManager abilities) {
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
        if (!AmethystToolFactory.is(item, AmethystToolType.WARDEN_WAND)) {
            return;
        }
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("warden-wand.enabled", true)) {
            return;
        }

        if (player.isSneaking()) {
            String mode = ModeStore.cycle(item, Keys.WARDEN_WAND_MODE, MODES);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(item);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
            player.updateInventory();
            player.sendActionBar(Component.text("Warden Wand mode: " + mode.toUpperCase(Locale.ROOT))
                    .color(NamedTextColor.AQUA));
            return;
        }

        switch (ModeStore.get(item, Keys.WARDEN_WAND_MODE, MODES[0])) {
            case "range" -> range(player);
            case "boss" -> boss(player);
            default -> summon(player);
        }
    }

    // --- summon: bats that chase and maul a target -------------------------

    private void summon(Player player) {
        double cost = plugin.getConfig().getDouble("warden-wand.summon.energy-cost", 30);
        long cd = plugin.getConfig().getLong("warden-wand.summon.cooldown-ms", 5000);
        if (!abilities.tryUse(player, "warden_summon", cost, cd)) {
            return;
        }
        int seconds = plugin.getConfig().getInt("warden-wand.summon.seconds", 8);
        double radius = plugin.getConfig().getDouble("warden-wand.summon.radius", 8);
        double dmgPerSecond = plugin.getConfig().getDouble("warden-wand.summon.damage", 1);
        int count = plugin.getConfig().getInt("warden-wand.summon.bats", 8);
        World world = player.getWorld();

        LivingEntity focus = aimedEnemy(player, 24);
        Location anchor = focus != null ? focus.getLocation() : player.getEyeLocation()
                .add(player.getEyeLocation().getDirection().multiply(6));

        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Bat bat = world.spawn(anchor.clone().add(rand(2), rand(2), rand(2)), Bat.class);
            bat.setAwake(true);
            bats.add(bat);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                LivingEntity victim = focus != null && focus.isValid() && !focus.isDead()
                        ? focus : nearestEnemy(anchor, player, radius);
                if (ticks >= seconds * 20 || !player.isOnline()) {
                    bats.forEach(b -> {
                        if (b.isValid()) {
                            b.remove();
                        }
                    });
                    cancel();
                    return;
                }
                // Bats dive at the victim.
                if (victim != null) {
                    for (Bat bat : bats) {
                        if (bat.isValid()) {
                            Vector dir = victim.getEyeLocation().toVector().subtract(bat.getLocation().toVector());
                            if (dir.lengthSquared() > 0.01) {
                                bat.setVelocity(dir.normalize().multiply(0.55));
                            }
                        }
                    }
                }
                // Apply the debuff + damage once a second.
                if (ticks % 20 == 0) {
                    Location centre = victim != null ? victim.getLocation() : anchor;
                    for (Entity e : world.getNearbyEntities(centre, radius, radius, radius)) {
                        if (e instanceof LivingEntity living && !e.equals(player)) {
                            living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                            living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                            living.setNoDamageTicks(0);
                            living.damage(dmgPerSecond, player);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        ToolDamage.damageMainHand(player);
    }

    // --- range: a volley of spectral arrows --------------------------------

    private void range(Player player) {
        int distance = plugin.getConfig().getInt("warden-wand.range.distance", 24);
        LivingEntity target = aimedEnemy(player, distance);
        if (target == null) {
            player.sendActionBar(Component.text("No target in sight.").color(NamedTextColor.GRAY));
            return;
        }
        double cost = plugin.getConfig().getDouble("warden-wand.range.energy-cost", 15);
        long cd = plugin.getConfig().getLong("warden-wand.range.cooldown-ms", 800);
        if (!abilities.tryUse(player, "warden_range", cost, cd)) {
            return;
        }
        double damage = plugin.getConfig().getDouble("warden-wand.range.damage", 8);
        int darknessTicks = plugin.getConfig().getInt("warden-wand.range.darkness-ticks", 200);
        long delay = plugin.getConfig().getLong("warden-wand.range.jumpscare-ticks", 12);

        // Spawn a bat right in the target's face (jumpscare), then it vanishes.
        World world = target.getWorld();
        Bat bat = world.spawn(target.getEyeLocation(), Bat.class);
        bat.setAwake(true);
        world.playSound(target.getEyeLocation(), "minecraft:entity.bat.takeoff", 1.2f, 0.6f);
        if (target instanceof Player victim) {
            victim.playSound(victim.getLocation(), "minecraft:entity.warden.nearby_closer", 1.0f, 1.0f);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (bat.isValid()) {
                bat.getWorld().spawnParticle(Particle.CLOUD, bat.getLocation(), 20, 0.3, 0.3, 0.3, 0.02);
                bat.remove();
            }
            if (target.isValid() && !target.isDead()) {
                target.setNoDamageTicks(0);
                target.damage(damage, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessTicks, 0));
            }
        }, delay);
        ToolDamage.damageMainHand(player);
    }

    // --- boss: the Warden Form ---------------------------------------------

    private void boss(Player player) {
        if (bossActive.contains(player.getUniqueId())) {
            player.sendActionBar(Component.text("You are already in Warden Form.").color(NamedTextColor.DARK_AQUA));
            return;
        }
        double fraction = plugin.getConfig().getDouble("warden-wand.boss.charge-fraction", 0.5);
        double required = abilities.maxEnergy() * Math.max(0.05, Math.min(1.0, fraction));
        if (abilities.energy(player) < required) {
            int pct = (int) (abilities.energy(player) / abilities.maxEnergy() * 100);
            player.sendActionBar(Component.text("Warden Form charging: " + pct + "% (need "
                    + (int) (fraction * 100) + "%)").color(NamedTextColor.RED));
            return;
        }
        if (!abilities.tryUse(player, "warden_boss", required,
                plugin.getConfig().getLong("warden-wand.boss.cooldown-ms", 60000))) {
            return;
        }

        int seconds = plugin.getConfig().getInt("warden-wand.boss.seconds", 45);
        double bonusHealth = plugin.getConfig().getDouble("warden-wand.boss.bonus-health", 40);
        double scaleBonus = plugin.getConfig().getDouble("warden-wand.boss.scale-bonus", 0.8);

        bossActive.add(player.getUniqueId());
        bossInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());

        applyModifier(player, "max_health", Keys.WARDEN_BOSS_HP, bonusHealth, bossHpModifier);
        applyModifier(player, "scale", Keys.WARDEN_BOSS_SCALE, scaleBonus, bossScaleModifier);
        AttributeInstance maxHealth = attributeInstance(player, "max_health");
        if (maxHealth != null) {
            player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + bonusHealth));
        }
        int duration = seconds * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
        player.getWorld().playSound(player.getLocation(), "minecraft:entity.warden.emerge", 2.0f, 1.0f);
        player.showTitle(Title.title(
                Component.text("⚠ WARDEN FORM").color(NamedTextColor.DARK_AQUA),
                Component.text("Unleash the deep dark").color(NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));

        double auraRadius = plugin.getConfig().getDouble("warden-wand.boss.aura-radius", 12);
        // Aura pulse only; this task self-cancels and never owns the revert.
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!bossActive.contains(player.getUniqueId()) || ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (ticks % 40 == 0) {
                    try {
                        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), auraRadius, auraRadius, auraRadius)) {
                            if (e instanceof LivingEntity living && !e.equals(player)) {
                                living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0));
                            }
                        }
                    } catch (Exception ignored) {
                        // Never let the aura kill the form.
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        // Guaranteed revert when the form ends — independent of the aura task.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> revertBoss(player), duration);
        ToolDamage.damageMainHand(player);
    }

    /** Safety net: revert if a boss-form player logs out. */
    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        revertBoss(event.getPlayer());
    }

    /** While in Warden Form, melee hits unleash a faked sonic boom. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && bossActive.contains(player.getUniqueId())) {
            sonicBoom(player);
        }
    }

    private void revertBoss(Player player) {
        if (!bossActive.remove(player.getUniqueId())) {
            return;
        }
        removeModifier(player, "max_health", bossHpModifier);
        removeModifier(player, "scale", bossScaleModifier);
        AttributeInstance maxHealth = attributeInstance(player, "max_health");
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.GLOWING);
        ItemStack[] saved = bossInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved);
            player.updateInventory();
        }
        player.sendActionBar(Component.text("The Warden's power fades.").color(NamedTextColor.GRAY));
    }

    private void sonicBoom(Player shooter) {
        World world = shooter.getWorld();
        Location start = shooter.getEyeLocation();
        Vector step = start.getDirection().normalize().multiply(0.75);
        Location point = start.clone();
        Set<Entity> hit = new HashSet<>();
        double damage = plugin.getConfig().getDouble("warden-wand.boss.sonic-damage", 8);
        world.playSound(start, "minecraft:entity.warden.sonic_boom", 1.5f, 1.0f);
        for (double d = 0; d <= 18; d += 0.75) {
            point.add(step);
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            for (Entity e : world.getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                if (!e.equals(shooter) && e instanceof LivingEntity living && hit.add(e)) {
                    living.setNoDamageTicks(0);
                    living.damage(damage, shooter);
                }
            }
        }
    }

    // --- helpers -----------------------------------------------------------

    private void applyModifier(Player player, String attrKey, NamespacedKey modKey, double amount,
                               Map<UUID, AttributeModifier> store) {
        AttributeInstance inst = attributeInstance(player, attrKey);
        if (inst == null) {
            return;
        }
        AttributeModifier mod = new AttributeModifier(modKey, amount,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        inst.addModifier(mod);
        store.put(player.getUniqueId(), mod);
    }

    private void removeModifier(Player player, String attrKey, Map<UUID, AttributeModifier> store) {
        AttributeInstance inst = attributeInstance(player, attrKey);
        AttributeModifier mod = store.remove(player.getUniqueId());
        if (inst != null && mod != null) {
            inst.removeModifier(mod);
        }
    }

    private AttributeInstance attributeInstance(Player player, String key) {
        Attribute attr = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(NamespacedKey.minecraft(key));
        return attr == null ? null : player.getAttribute(attr);
    }

    private LivingEntity aimedEnemy(Player player, int distance) {
        RayTraceResult ray = player.rayTraceEntities(distance);
        return ray != null && ray.getHitEntity() instanceof LivingEntity le && !le.equals(player) ? le : null;
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

    private Vector rotateYaw(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vector(vector.getX() * cos - vector.getZ() * sin, vector.getY(),
                vector.getX() * sin + vector.getZ() * cos);
    }

    private double rand(double range) {
        return (Math.random() - 0.5) * 2 * range;
    }
}
