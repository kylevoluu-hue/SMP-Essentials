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
import org.bukkit.plugin.java.JavaPlugin;

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

        Messages messages = new Messages(this);
        this.combatManager = new CombatTagManager(this, messages);

        // Listeners
        getServer().getPluginManager().registerEvents(new AreaMiningListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeFellerListener(this), this);
        getServer().getPluginManager().registerEvents(new ToolSoundListener(this), this);
        getServer().getPluginManager().registerEvents(
                new CombatLogListener(this, combatManager, messages), this);

        // Recipes
        if (getConfig().getBoolean("amethyst-tools.recipes-enabled", true)) {
            ToolRegistry.register();
        }

        // Command
        PluginCommand command = getCommand("smpe");
        if (command != null) {
            SmpCommand handler = new SmpCommand(this, combatManager, messages);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        // Combat action-bar / expiry ticker
        combatManager.start();

        getLogger().info("SMP Essentials enabled (Amethyst tools + anti-combat-log).");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.stop();
        }
        ToolRegistry.unregister();
    }

    /** Reload config and re-apply recipes + scheduler. Invoked by /smpe reload. */
    public void reloadPlugin() {
        reloadConfig();
        ToolRegistry.unregister();
        if (getConfig().getBoolean("amethyst-tools.recipes-enabled", true)) {
            ToolRegistry.register();
        }
        if (combatManager != null) {
            combatManager.start();
        }
    }
}
