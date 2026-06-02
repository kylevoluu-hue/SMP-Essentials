package io.github.kylevoluu.smpessentials.ability;

import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared per-player "ability energy" pool plus per-ability cooldowns for the
 * custom ability items. Energy regenerates over time toward a cap and is shown as
 * a boss bar while a player holds an ability item. Each ability checks
 * {@link #tryUse} (cooldown free + enough energy) before firing.
 */
public final class AbilityManager {

    private final Plugin plugin;
    private final Map<UUID, Double> energy = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public AbilityManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public double maxEnergy() {
        return Math.max(1, plugin.getConfig().getDouble("abilities.max-energy", 100));
    }

    private double regenPerSecond() {
        return Math.max(0, plugin.getConfig().getDouble("abilities.regen-per-second", 10));
    }

    public double energy(Player player) {
        return energy.getOrDefault(player.getUniqueId(), maxEnergy());
    }

    private void setEnergy(Player player, double value) {
        energy.put(player.getUniqueId(), Math.max(0, Math.min(maxEnergy(), value)));
    }

    /** True if the ability key is off cooldown for this player. */
    public boolean ready(Player player, String key) {
        Map<String, Long> map = cooldownExpiry.get(player.getUniqueId());
        return map == null || !map.containsKey(key) || System.currentTimeMillis() >= map.get(key);
    }

    /**
     * Attempt to use an ability: checks cooldown and energy, and if both pass,
     * consumes the energy and arms the cooldown. Sends action-bar feedback on fail.
     */
    public boolean tryUse(Player player, String key, double cost, long cooldownMs) {
        if (!ready(player, key)) {
            long remaining = cooldownExpiry.get(player.getUniqueId()).get(key) - System.currentTimeMillis();
            player.sendActionBar(Component.text("On cooldown: " + Math.max(1, remaining / 1000) + "s")
                    .color(NamedTextColor.RED));
            return false;
        }
        if (energy(player) < cost) {
            player.sendActionBar(Component.text("Not enough energy (recharging)").color(NamedTextColor.RED));
            return false;
        }
        setEnergy(player, energy(player) - cost);
        cooldownExpiry.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(key, System.currentTimeMillis() + cooldownMs);
        return true;
    }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 10L, 10L); // every 0.5s
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID id : new HashMap<>(bars).keySet()) {
            hideBar(id);
        }
    }

    private void tick() {
        boolean showBar = plugin.getConfig().getBoolean("abilities.bossbar", true);
        double regen = regenPerSecond() * 0.5; // task runs twice per second
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            setEnergy(player, energy(player) + regen);

            if (showBar && holdingAbilityItem(player)) {
                float progress = (float) Math.max(0, Math.min(1, energy(player) / maxEnergy()));
                BossBar bar = bars.get(id);
                Component title = Component.text("Ability Energy: " + (int) energy(player) + "/" + (int) maxEnergy());
                if (bar == null) {
                    bar = BossBar.bossBar(title, progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                    bars.put(id, bar);
                    player.showBossBar(bar);
                } else {
                    bar.name(title);
                    bar.progress(progress);
                }
            } else if (bars.containsKey(id)) {
                hideBar(id);
            }
        }
    }

    private boolean holdingAbilityItem(Player player) {
        return isAbility(player.getInventory().getItemInMainHand())
                || isAbility(player.getInventory().getItemInOffHand());
    }

    private boolean isAbility(ItemStack item) {
        AmethystToolType type = AmethystToolFactory.typeOf(item);
        return type != null && type.customModelData() > 0;
    }

    private void hideBar(UUID id) {
        BossBar bar = bars.remove(id);
        if (bar != null) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }
}
