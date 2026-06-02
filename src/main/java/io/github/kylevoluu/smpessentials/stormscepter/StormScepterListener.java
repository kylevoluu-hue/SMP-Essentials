package io.github.kylevoluu.smpessentials.stormscepter;

import io.github.kylevoluu.smpessentials.ability.AbilityManager;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ModeStore;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Locale;

/** The Storm Scepter: modes lightning-bow / sunken / summon (sneak to cycle). */
public final class StormScepterListener implements Listener {

    private static final String[] MODES = {"lightning", "sunken", "summon"};

    private final Plugin plugin;
    private final AbilityManager abilities;

    public StormScepterListener(Plugin plugin, AbilityManager abilities) {
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
        if (!AmethystToolFactory.is(item, AmethystToolType.STORM_SCEPTER)) {
            return;
        }
        event.setCancelled(true);
        if (!plugin.getConfig().getBoolean("storm-scepter.enabled", true)) {
            return;
        }

        if (player.isSneaking()) {
            String mode = ModeStore.cycle(item, Keys.SCEPTER_MODE, MODES);
            setHand(player, hand, item);
            player.sendActionBar(Component.text("Scepter mode: " + mode.toUpperCase(Locale.ROOT)));
            return;
        }

        switch (ModeStore.get(item, Keys.SCEPTER_MODE, MODES[0])) {
            case "sunken" -> sunken(player);
            case "summon" -> summon(player);
            default -> lightning(player);
        }
    }

    private void lightning(Player player) {
        World world = player.getWorld();
        if (!world.hasStorm() && !world.isThundering() && !hasFrightening(player)) {
            player.sendActionBar(Component.text(
                    "The Storm Scepter needs a storm (or the 'Very Very Frightening' advancement)."));
            return;
        }
        double cost = plugin.getConfig().getDouble("storm-scepter.lightning.energy-cost", 20);
        long cd = plugin.getConfig().getLong("storm-scepter.lightning.cooldown-ms", 1200);
        if (!abilities.tryUse(player, "scepter_lightning", cost, cd)) {
            return;
        }
        double range = plugin.getConfig().getDouble("storm-scepter.lightning.range", 40);
        RayTraceResult ray = player.rayTraceBlocks(range);
        Location target = ray != null && ray.getHitPosition() != null
                ? ray.getHitPosition().toLocation(world)
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        world.strikeLightning(target);
        ToolDamage.damageMainHand(player);
    }

    private void sunken(Player player) {
        RayTraceResult ray = player.rayTraceEntities(20);
        if (ray == null || !(ray.getHitEntity() instanceof LivingEntity target)) {
            player.sendActionBar(Component.text("No target for Sunken."));
            return;
        }
        double cost = plugin.getConfig().getDouble("storm-scepter.sunken.energy-cost", 30);
        long cd = plugin.getConfig().getLong("storm-scepter.sunken.cooldown-ms", 4000);
        if (!abilities.tryUse(player, "scepter_sunken", cost, cd)) {
            return;
        }
        double damage = plugin.getConfig().getDouble("storm-scepter.sunken.damage", 8);
        target.setVelocity(new Vector(0, 1.4, 0));
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 60, 0.6, 1.0, 0.6, 0.1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid() && !target.isDead()) {
                target.setVelocity(new Vector(0, -2.2, 0));
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 80, 0.8, 0.5, 0.8, 0.2);
                target.setNoDamageTicks(0);
                target.damage(damage);
            }
        }, 18L);
        ToolDamage.damageMainHand(player);
    }

    private void summon(Player player) {
        double cost = plugin.getConfig().getDouble("storm-scepter.summon.energy-cost", 35);
        long cd = plugin.getConfig().getLong("storm-scepter.summon.cooldown-ms", 6000);
        if (!abilities.tryUse(player, "scepter_summon", cost, cd)) {
            return;
        }
        int seconds = plugin.getConfig().getInt("storm-scepter.summon.seconds", 20);
        double vexDamage = plugin.getConfig().getDouble("storm-scepter.summon.vex-damage", 2);
        RayTraceResult ray = player.rayTraceEntities(24);
        LivingEntity target = ray != null && ray.getHitEntity() instanceof LivingEntity le ? le : null;

        Vex vex = player.getWorld().spawn(player.getEyeLocation(), Vex.class, v -> {
            if (target != null) {
                v.setTarget(target);
            }
            Attribute attackDamage = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                    .get(NamespacedKey.minecraft("attack_damage"));
            AttributeInstance attr = attackDamage == null ? null : v.getAttribute(attackDamage);
            if (attr != null) {
                attr.setBaseValue(vexDamage);
            }
        });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (vex.isValid()) {
                vex.remove();
            }
        }, seconds * 20L);
        ToolDamage.damageMainHand(player);
    }

    private boolean hasFrightening(Player player) {
        Advancement adv = Bukkit.getAdvancement(NamespacedKey.minecraft("adventure/very_very_frightening"));
        return adv != null && player.getAdvancementProgress(adv).isDone();
    }

    private void setHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
        player.updateInventory();
    }
}
