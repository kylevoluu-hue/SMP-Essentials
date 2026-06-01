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
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The Warden Wand: modes summon (bats) / range (spectral arrows) / boss (morph). */
public final class WardenWandListener implements Listener {

    private static final String[] MODES = {"summon", "range", "boss"};

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Map<UUID, ItemStack[]> bossInventories = new HashMap<>();
    private final Map<UUID, AttributeModifier> bossHpModifier = new HashMap<>();
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
            player.sendActionBar(Component.text("Warden Wand mode: " + mode.toUpperCase(Locale.ROOT)));
            return;
        }

        switch (ModeStore.get(item, Keys.WARDEN_WAND_MODE, MODES[0])) {
            case "range" -> range(player);
            case "boss" -> boss(player);
            default -> summon(player);
        }
    }

    private void summon(Player player) {
        double cost = plugin.getConfig().getDouble("warden-wand.summon.energy-cost", 30);
        long cd = plugin.getConfig().getLong("warden-wand.summon.cooldown-ms", 5000);
        if (!abilities.tryUse(player, "warden_summon", cost, cd)) {
            return;
        }
        int seconds = plugin.getConfig().getInt("warden-wand.summon.seconds", 8);
        double radius = plugin.getConfig().getDouble("warden-wand.summon.radius", 8);
        double dmgPerSecond = plugin.getConfig().getDouble("warden-wand.summon.damage", 1);
        World world = player.getWorld();
        Set<org.bukkit.entity.Bat> bats = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            bats.add(world.spawn(player.getLocation().add(0, 1, 0), org.bukkit.entity.Bat.class));
        }
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= seconds * 20 || !player.isOnline()) {
                    bats.forEach(b -> {
                        if (b.isValid()) {
                            b.remove();
                        }
                    });
                    cancel();
                    return;
                }
                if (ticks % 20 == 0) {
                    for (Entity e : world.getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                        if (e instanceof LivingEntity living && !e.equals(player)) {
                            living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                            living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                            living.setNoDamageTicks(0);
                            living.damage(dmgPerSecond);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        ToolDamage.damageMainHand(player);
    }

    private void range(Player player) {
        double cost = plugin.getConfig().getDouble("warden-wand.range.energy-cost", 15);
        long cd = plugin.getConfig().getLong("warden-wand.range.cooldown-ms", 800);
        if (!abilities.tryUse(player, "warden_range", cost, cd)) {
            return;
        }
        SpectralArrow arrow = player.launchProjectile(SpectralArrow.class, player.getEyeLocation().getDirection());
        arrow.setGlowingTicks(200);
        arrow.getPersistentDataContainer().set(Keys.WARDEN_ARROW, PersistentDataType.BYTE, (byte) 1);
        ToolDamage.damageMainHand(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof SpectralArrow arrow
                && arrow.getPersistentDataContainer().has(Keys.WARDEN_ARROW, PersistentDataType.BYTE)) {
            event.setDamage(plugin.getConfig().getDouble("warden-wand.range.arrow-damage", 4));
        }
    }

    private void boss(Player player) {
        if (bossActive.contains(player.getUniqueId())) {
            return;
        }
        // Requires a (near) full charge: consume essentially all energy.
        double required = abilities.maxEnergy() * 0.95;
        if (!abilities.tryUse(player, "warden_boss", required,
                plugin.getConfig().getLong("warden-wand.boss.cooldown-ms", 60000))) {
            return;
        }
        int seconds = plugin.getConfig().getInt("warden-wand.boss.seconds", 45);
        double bonusHealth = plugin.getConfig().getDouble("warden-wand.boss.bonus-health", 40);

        bossActive.add(player.getUniqueId());
        bossInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());

        AttributeInstance maxHealth = maxHealth(player);
        if (maxHealth != null) {
            AttributeModifier mod = new AttributeModifier(Keys.WARDEN_BOSS_HP, bonusHealth,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
            maxHealth.addModifier(mod);
            bossHpModifier.put(player.getUniqueId(), mod);
            player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + bonusHealth));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, seconds * 20, 1));
        player.getWorld().playSound(player.getLocation(), "minecraft:entity.warden.emerge", 2.0f, 1.0f);

        double auraRadius = plugin.getConfig().getDouble("warden-wand.boss.aura-radius", 12);
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= seconds * 20 || !player.isOnline() || !bossActive.contains(player.getUniqueId())) {
                    revertBoss(player);
                    cancel();
                    return;
                }
                if (ticks % 40 == 0) {
                    for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), auraRadius, auraRadius, auraRadius)) {
                        if (e instanceof LivingEntity living && !e.equals(player)) {
                            living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        ToolDamage.damageMainHand(player);
    }

    /** While boss-active, melee hits unleash a faked sonic boom. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !bossActive.contains(player.getUniqueId())) {
            return;
        }
        sonicBoom(player);
    }

    private void revertBoss(Player player) {
        if (!bossActive.remove(player.getUniqueId())) {
            return;
        }
        AttributeInstance maxHealth = maxHealth(player);
        AttributeModifier mod = bossHpModifier.remove(player.getUniqueId());
        if (maxHealth != null && mod != null) {
            maxHealth.removeModifier(mod);
            if (player.getHealth() > maxHealth.getValue()) {
                player.setHealth(maxHealth.getValue());
            }
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
        ItemStack[] saved = bossInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved);
            player.updateInventory();
        }
        player.sendActionBar(Component.text("The Warden's power fades."));
    }

    private void sonicBoom(Player shooter) {
        World world = shooter.getWorld();
        Location start = shooter.getEyeLocation();
        Vector step = start.getDirection().normalize().multiply(0.75);
        Location point = start.clone();
        Set<Entity> hit = new HashSet<>();
        world.playSound(start, "minecraft:entity.warden.sonic_boom", 1.5f, 1.0f);
        for (double d = 0; d <= 18; d += 0.75) {
            point.add(step);
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            for (Entity e : world.getNearbyEntities(point, 1.4, 1.4, 1.4)) {
                if (!e.equals(shooter) && e instanceof LivingEntity living && hit.add(e)) {
                    living.damage(8, shooter);
                }
            }
        }
    }

    private AttributeInstance maxHealth(Player player) {
        Attribute attr = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(NamespacedKey.minecraft("max_health"));
        return attr == null ? null : player.getAttribute(attr);
    }
}
