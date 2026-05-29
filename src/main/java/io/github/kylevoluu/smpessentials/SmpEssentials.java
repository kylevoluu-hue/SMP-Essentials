package io.github.kylevoluu.smpessentials;

import io.github.kylevoluu.smpessentials.combatlog.CombatLogListener;
import io.github.kylevoluu.smpessentials.combatlog.CombatTagManager;
import io.github.kylevoluu.smpessentials.command.SmpCommand;
import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.mining.AreaMiningListener;
import io.github.kylevoluu.smpessentials.tools.ToolRegistry;
import io.github.kylevoluu.smpessentials.tools.ToolSoundListener;
import io.github.kylevoluu.smpessentials.treefeller.TreeFellerListener;
import io.github.kylevoluu.smpessentials.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;

/**
 * SMP Essentials entry point: Amethyst tools (3x3 pickaxe, tree-felling axe) and
 * an anti-combat-log system, for Paper 26.1.2.
 */
public final class SmpEssentials extends JavaPlugin {

    private CombatTagManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);
        applyConfiguredPreset();

        Messages messages = new Messages(this);
        this.combatManager = new CombatTagManager(this, messages);

        // Listeners
        getServer().getPluginManager().registerEvents(new AreaMiningListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeFellerListener(this), this);
        getServer().getPluginManager().registerEvents(new ToolSoundListener(this), this);
        getServer().getPluginManager().registerEvents(
                new CombatLogListener(this, combatManager, messages), this);

        // Command
        PluginCommand command = getCommand("smpe");
        if (command != null) {
            SmpCommand handler = new SmpCommand(this, combatManager, messages);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        // Recipes + combat ticker
        refreshRuntime();

        getLogger().info("SMP Essentials enabled (Amethyst tools + anti-combat-log).");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.stop();
        }
        ToolRegistry.unregister();
    }

    /** Reload config, re-apply the active preset, and refresh recipes + scheduler. */
    public void reloadPlugin() {
        reloadConfig();
        applyConfiguredPreset();
        refreshRuntime();
    }

    // --- Presets ------------------------------------------------------------

    /** Names of the presets defined in config.yml. */
    public Set<String> presetNames() {
        ConfigurationSection presets = getConfig().getConfigurationSection("presets");
        return presets == null ? Collections.emptySet() : presets.getKeys(false);
    }

    /** The preset currently selected via {@code active-preset}. */
    public String activePreset() {
        return getConfig().getString("active-preset", "custom");
    }

    /**
     * Apply a named preset's values live (in memory) and select it as active,
     * then refresh recipes and the combat ticker. Returns {@code false} if the
     * preset does not exist.
     */
    public boolean applyPresetByName(String name) {
        if (!applyPreset(name)) {
            return false;
        }
        getConfig().set("active-preset", name);
        refreshRuntime();
        return true;
    }

    /** Read {@code active-preset} and apply it, unless it is custom/none. */
    private void applyConfiguredPreset() {
        String name = activePreset();
        if (name == null || name.isBlank()
                || name.equalsIgnoreCase("custom") || name.equalsIgnoreCase("none")) {
            return;
        }
        if (!applyPreset(name)) {
            getLogger().warning("Unknown preset '" + name + "'; using config.yml values as-is.");
        }
    }

    /** Copy every leaf value from {@code presets.<name>} over the live config. */
    private boolean applyPreset(String name) {
        ConfigurationSection preset = getConfig().getConfigurationSection("presets." + name);
        if (preset == null) {
            return false;
        }
        for (String path : preset.getKeys(true)) {
            if (!preset.isConfigurationSection(path)) {
                getConfig().set(path, preset.get(path));
            }
        }
        getLogger().info("Applied preset '" + name + "'.");
        return true;
    }

    /** Re-register recipes (per config) and (re)start the combat ticker. */
    private void refreshRuntime() {
        ToolRegistry.unregister();
        if (getConfig().getBoolean("amethyst-tools.recipes-enabled", true)) {
            ToolRegistry.register();
        }
        if (combatManager != null) {
            combatManager.start();
        }
    }
}
