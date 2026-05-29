package io.github.kylevoluu.smpessentials.combatlog;

import io.github.kylevoluu.smpessentials.util.Messages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;

/**
 * Drives the anti-combat-log system: tagging on PvP, blocking escape commands /
 * teleports while tagged, punishing players who disconnect in combat, and
 * cleaning them up when they rejoin.
 */
public final class CombatLogListener implements Listener {

    private final Plugin plugin;
    private final CombatTagManager combat;
    private final Messages messages;

    public CombatLogListener(Plugin plugin, CombatTagManager combat, Messages messages) {
        this.plugin = plugin;
        this.combat = combat;
        this.messages = messages;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("combat.enabled", true);
    }

    private boolean bypasses(Player player) {
        return player.hasPermission("smpessentials.combat.bypass");
    }

    // --- Tagging ------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Combat tagging is STRICTLY player-vs-player: the timer is only ever
        // started or refreshed by a player hitting another player (melee or a
        // player-fired projectile). Mob and environmental damage never tag.
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        // tag() re-stamps the expiry to the full duration, so every new hit
        // resets the countdown back to combat.duration-seconds (20 by default).
        if (!bypasses(attacker)) {
            combat.tag(attacker);
        }
        if (!bypasses(victim)) {
            combat.tag(victim);
        }
    }

    /** A direct player attacker or the player who fired a projectile, else null. */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // --- Blocking while tagged ---------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-commands", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!combat.isTagged(player.getUniqueId()) || bypasses(player)) {
            return;
        }

        String root = event.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        for (String blocked : plugin.getConfig().getStringList("combat.blocked-commands")) {
            if (root.equals(blocked.toLowerCase(Locale.ROOT))) {
                event.setCancelled(true);
                player.sendMessage(messages.prefixed("combat-command-blocked", "command", root));
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!combat.isTagged(player.getUniqueId()) || bypasses(player)) {
            return;
        }
        if (isTeleportBlocked(event.getCause())) {
            event.setCancelled(true);
            player.sendMessage(messages.prefixed("combat-teleport-blocked"));
        }
    }

    /** Whether this teleport cause is blocked, via its dedicated toggle or the generic list. */
    private boolean isTeleportBlocked(PlayerTeleportEvent.TeleportCause cause) {
        String name = cause.name();
        if (name.equals("ENDER_PEARL") && plugin.getConfig().getBoolean("combat.block-ender-pearl", false)) {
            return true;
        }
        if (name.equals("CHORUS_FRUIT") && plugin.getConfig().getBoolean("combat.block-chorus-fruit", false)) {
            return true;
        }
        if (name.equals("END_GATEWAY") && plugin.getConfig().getBoolean("combat.block-end-gateway", false)) {
            return true;
        }
        if (name.equals("EXIT_BED") && plugin.getConfig().getBoolean("combat.block-exit-bed", false)) {
            return true;
        }
        // Generic cause list (COMMAND/PLUGIN/SPECTATE etc.).
        if (plugin.getConfig().getBoolean("combat.block-teleports", true)) {
            for (String blocked : plugin.getConfig().getStringList("combat.blocked-teleport-causes")) {
                if (name.equalsIgnoreCase(blocked)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-elytra", false)) {
            return;
        }
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return; // Only block starting to glide.
        }
        if (combat.isTagged(player.getUniqueId()) && !bypasses(player)) {
            event.setCancelled(true);
            player.sendMessage(messages.prefixed("combat-elytra-blocked"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlight(PlayerToggleFlightEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-flight", false)) {
            return;
        }
        if (!event.isFlying()) {
            return; // Only block starting to fly.
        }
        Player player = event.getPlayer();
        if (combat.isTagged(player.getUniqueId()) && !bypasses(player)) {
            event.setCancelled(true);
            player.sendMessage(messages.prefixed("combat-flight-blocked"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        // Covers every rideable transport: boats, minecarts, horses, pigs,
        // striders, camels, llamas, etc. (they are all mounted entities).
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-mounting", false)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (combat.isTagged(player.getUniqueId()) && !bypasses(player)) {
            event.setCancelled(true);
            player.sendMessage(messages.prefixed("combat-mount-blocked"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-dismounting", false)) {
            return;
        }
        if (!(event.getExited() instanceof Player player)) {
            return;
        }
        if (combat.isTagged(player.getUniqueId()) && !bypasses(player)) {
            event.setCancelled(true);
            player.sendMessage(messages.prefixed("combat-dismount-blocked"));
        }
    }

    // --- Combat logging out -------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!combat.isTagged(player.getUniqueId()) || bypasses(player)) {
            return;
        }

        punish(player);
        combat.untag(player.getUniqueId());
        combat.markCombatLogger(player.getUniqueId());

        plugin.getServer().broadcast(messages.prefixed("combat-log-broadcast", "player", player.getName()));
    }

    /** Drop the player's loot (and XP) at their location, then wipe their state. */
    private void punish(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        PlayerInventory inventory = player.getInventory();

        if (world != null) {
            // getStorageContents() is the 36 main slots only; armor and offhand
            // are dropped separately below to avoid duplicating them.
            for (ItemStack item : inventory.getStorageContents()) {
                dropIfPresent(world, location, item);
            }
            for (ItemStack armor : inventory.getArmorContents()) {
                dropIfPresent(world, location, armor);
            }
            dropIfPresent(world, location, inventory.getItemInOffHand());

            if (plugin.getConfig().getBoolean("combat.drop-experience", true)) {
                int xp = totalExperience(player);
                if (xp > 0) {
                    world.spawn(location, ExperienceOrb.class, orb -> orb.setExperience(xp));
                }
            }
        }

        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setFoodLevel(20);
    }

    private void dropIfPresent(World world, Location location, ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            world.dropItemNaturally(location, item);
        }
    }

    /** Compute the player's total stored experience points (level + progress). */
    private int totalExperience(Player player) {
        int level = player.getLevel();
        int fromLevels = expAtLevel(level);
        int withinLevel = Math.round(player.getExp() * player.getExpToLevel());
        return fromLevels + withinLevel;
    }

    /** Total XP points required to reach the start of the given level (vanilla formula). */
    private int expAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    // --- Rejoin cleanup -----------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!combat.consumeCombatLogger(player.getUniqueId())) {
            return;
        }
        player.sendMessage(messages.prefixed("combat-log-rejoin"));
        if (plugin.getConfig().getBoolean("combat.teleport-to-spawn-on-rejoin", true)) {
            World world = player.getWorld();
            player.teleport(world.getSpawnLocation());
        }
    }
}
