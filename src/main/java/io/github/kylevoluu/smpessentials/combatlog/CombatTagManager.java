package io.github.kylevoluu.smpessentials.combatlog;

import io.github.kylevoluu.smpessentials.util.Messages;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are in combat (with an expiry timestamp) and which
 * players combat-logged and still owe a "you were slain" message on rejoin.
 */
public final class CombatTagManager {

    private final Plugin plugin;
    private final Messages messages;

    /** UUID -> epoch millis at which combat ends. */
    private final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

    /** Players who combat-logged and have not yet rejoined (persisted across restarts). */
    private final Set<UUID> pendingLoggers = new HashSet<>();

    /** Active combat boss bars by player UUID. */
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private final File loggerFile;
    private BukkitTask task;

    public CombatTagManager(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.loggerFile = new File(plugin.getDataFolder(), "combat-loggers.yml");
        loadLoggers();
    }

    private int durationSeconds() {
        return Math.max(1, plugin.getConfig().getInt("combat.duration-seconds", 20));
    }

    /** Put (or refresh) a player in combat. Each call resets the timer to the full duration. */
    public void tag(Player player) {
        UUID id = player.getUniqueId();
        boolean wasTagged = isTagged(id);
        combatExpiry.put(id, System.currentTimeMillis() + durationSeconds() * 1000L);
        if (!wasTagged) {
            player.sendMessage(messages.prefixed("combat-entered", "seconds", String.valueOf(durationSeconds())));
        }
    }

    public boolean isTagged(UUID id) {
        Long expiry = combatExpiry.get(id);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiry) {
            combatExpiry.remove(id);
            return false;
        }
        return true;
    }

    public int remainingSeconds(UUID id) {
        Long expiry = combatExpiry.get(id);
        if (expiry == null) {
            return 0;
        }
        long remainingMs = expiry - System.currentTimeMillis();
        return remainingMs <= 0 ? 0 : (int) Math.ceil(remainingMs / 1000.0);
    }

    public void untag(UUID id) {
        combatExpiry.remove(id);
        removeBossBar(id);
    }

    /** Start the action-bar / boss-bar / expiry ticker (runs 5x per second for a smooth bar). */
    public void start() {
        cancelTask();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

    /** Stop the ticker and clear every active boss bar (used on disable). */
    public void stop() {
        cancelTask();
        clearAllBars();
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        boolean actionBar = plugin.getConfig().getBoolean("combat.action-bar", true);
        boolean bossBar = plugin.getConfig().getBoolean("combat.boss-bar", true);
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : new ArrayList<>(combatExpiry.entrySet())) {
            UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);

            if (now >= entry.getValue()) {
                combatExpiry.remove(id);
                removeBossBar(id);
                if (player != null) {
                    player.sendActionBar(net.kyori.adventure.text.Component.empty());
                    player.sendMessage(messages.prefixed("combat-expired"));
                }
                continue;
            }

            if (player == null) {
                continue;
            }
            int seconds = remainingSeconds(id);
            if (actionBar) {
                player.sendActionBar(messages.plain("combat-action-bar", "seconds", String.valueOf(seconds)));
            }
            if (bossBar) {
                float progress = clampProgress((entry.getValue() - now) / (durationSeconds() * 1000f));
                updateBossBar(player, seconds, progress);
            } else {
                removeBossBar(id);
            }
        }

        // Drop any stray bars for players no longer tagged.
        for (UUID id : new ArrayList<>(bossBars.keySet())) {
            if (!combatExpiry.containsKey(id)) {
                removeBossBar(id);
            }
        }
    }

    private void updateBossBar(Player player, int seconds, float progress) {
        net.kyori.adventure.text.Component title =
                messages.plain("combat-boss-bar", "seconds", String.valueOf(seconds));
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, bossBarColor(), BossBar.Overlay.PROGRESS);
            bossBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(progress);
            bar.color(bossBarColor());
        }
    }

    private void removeBossBar(UUID id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private void clearAllBars() {
        for (UUID id : new ArrayList<>(bossBars.keySet())) {
            removeBossBar(id);
        }
    }

    private BossBar.Color bossBarColor() {
        String raw = plugin.getConfig().getString("combat.boss-bar-color", "RED");
        try {
            return BossBar.Color.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    private static float clampProgress(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }

    // --- combat-logger persistence -----------------------------------------

    public void markCombatLogger(UUID id) {
        pendingLoggers.add(id);
        saveLoggers();
    }

    public boolean consumeCombatLogger(UUID id) {
        boolean was = pendingLoggers.remove(id);
        if (was) {
            saveLoggers();
        }
        return was;
    }

    private void loadLoggers() {
        if (!loggerFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(loggerFile);
        for (String raw : yaml.getStringList("combat-loggers")) {
            try {
                pendingLoggers.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries.
            }
        }
    }

    private void saveLoggers() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        for (UUID id : pendingLoggers) {
            ids.add(id.toString());
        }
        yaml.set("combat-loggers", ids);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(loggerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save combat-loggers.yml: " + e.getMessage());
        }
    }

    /** Snapshot of currently-tagged players, for shutdown handling. */
    public Set<UUID> taggedPlayers() {
        return new HashSet<>(combatExpiry.keySet());
    }
}
