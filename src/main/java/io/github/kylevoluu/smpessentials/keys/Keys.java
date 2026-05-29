package io.github.kylevoluu.smpessentials.keys;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central holder for all {@link NamespacedKey}s used by the plugin. Initialised
 * once from the plugin instance so keys share a consistent namespace.
 */
public final class Keys {

    /** PersistentDataContainer key that marks an item as an Amethyst tool. */
    public static NamespacedKey TOOL;

    /** Recipe key for the Amethyst Pickaxe. */
    public static NamespacedKey RECIPE_PICKAXE;

    /** Recipe key for the Amethyst Axe. */
    public static NamespacedKey RECIPE_AXE;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        TOOL = new NamespacedKey(plugin, "amethyst_tool");
        RECIPE_PICKAXE = new NamespacedKey(plugin, "amethyst_pickaxe");
        RECIPE_AXE = new NamespacedKey(plugin, "amethyst_axe");
    }
}
