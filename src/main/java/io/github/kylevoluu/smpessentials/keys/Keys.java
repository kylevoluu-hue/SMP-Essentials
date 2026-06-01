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

    /** Recipe keys for the Shovel, Sword and Bucket. */
    public static NamespacedKey RECIPE_SHOVEL;
    public static NamespacedKey RECIPE_SWORD;
    public static NamespacedKey RECIPE_BUCKET;
    public static NamespacedKey RECIPE_BLAZE_SWORD;

    /** Marks a falling block spawned by the Amethyst Sword. */
    public static NamespacedKey DRIPSTONE;

    /** Marks a fireball shot by the Blaze Sword. */
    public static NamespacedKey BLAZE_FIREBALL;

    /** Attribute-modifier keys for the Amethyst Sword. */
    public static NamespacedKey SWORD_ATTACK_DAMAGE;
    public static NamespacedKey SWORD_ATTACK_SPEED;

    /** Amethyst Bucket stored amounts and selected pour mode. */
    public static NamespacedKey BUCKET_WATER;
    public static NamespacedKey BUCKET_LAVA;
    public static NamespacedKey BUCKET_MODE;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        TOOL = new NamespacedKey(plugin, "amethyst_tool");
        RECIPE_PICKAXE = new NamespacedKey(plugin, "amethyst_pickaxe");
        RECIPE_AXE = new NamespacedKey(plugin, "amethyst_axe");
        RECIPE_SHOVEL = new NamespacedKey(plugin, "amethyst_shovel");
        RECIPE_SWORD = new NamespacedKey(plugin, "amethyst_sword");
        RECIPE_BUCKET = new NamespacedKey(plugin, "amethyst_bucket");
        RECIPE_BLAZE_SWORD = new NamespacedKey(plugin, "blaze_sword");
        DRIPSTONE = new NamespacedKey(plugin, "sword_dripstone");
        BLAZE_FIREBALL = new NamespacedKey(plugin, "blaze_fireball");
        SWORD_ATTACK_DAMAGE = new NamespacedKey(plugin, "sword_attack_damage");
        SWORD_ATTACK_SPEED = new NamespacedKey(plugin, "sword_attack_speed");
        BUCKET_WATER = new NamespacedKey(plugin, "bucket_water");
        BUCKET_LAVA = new NamespacedKey(plugin, "bucket_lava");
        BUCKET_MODE = new NamespacedKey(plugin, "bucket_mode");
    }
}
