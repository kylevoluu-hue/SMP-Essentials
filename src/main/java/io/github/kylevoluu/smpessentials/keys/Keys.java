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
    public static NamespacedKey RECIPE_BLAZE_WAND;
    public static NamespacedKey RECIPE_STORM_ROD;

    /** Ability-item recipe keys. */
    public static NamespacedKey RECIPE_WARDEN_CROSSBOW;
    public static NamespacedKey RECIPE_STORM_SCEPTER;
    public static NamespacedKey RECIPE_WARDEN_WAND;
    public static NamespacedKey RECIPE_ENDER_WAND;
    public static NamespacedKey RECIPE_TROLL_STAFF;
    public static NamespacedKey RECIPE_CLONING_CANE;

    /** Per-player mob-kill counter (Cloning Cane charge). */
    public static NamespacedKey MOB_KILLS;

    /** Mode markers for the multi-mode ability items. */
    public static NamespacedKey SCEPTER_MODE;
    public static NamespacedKey WARDEN_WAND_MODE;
    public static NamespacedKey ENDER_WAND_MODE;
    public static NamespacedKey CLONING_MODE;

    /** Marks a falling block spawned by the Amethyst Sword. */
    public static NamespacedKey DRIPSTONE;

    /** Marks a fireball shot by the Blaze Sword. */
    public static NamespacedKey BLAZE_FIREBALL;

    /** Blaze Wand: selected mode, and markers for its fireballs and TNT. */
    public static NamespacedKey WAND_MODE;
    public static NamespacedKey WAND_FIREBALL;
    public static NamespacedKey WAND_TNT;

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
        RECIPE_BLAZE_WAND = new NamespacedKey(plugin, "blaze_wand");
        RECIPE_STORM_ROD = new NamespacedKey(plugin, "storm_rod");
        RECIPE_WARDEN_CROSSBOW = new NamespacedKey(plugin, "warden_crossbow");
        RECIPE_STORM_SCEPTER = new NamespacedKey(plugin, "storm_scepter");
        RECIPE_WARDEN_WAND = new NamespacedKey(plugin, "warden_wand");
        RECIPE_ENDER_WAND = new NamespacedKey(plugin, "ender_wand");
        RECIPE_TROLL_STAFF = new NamespacedKey(plugin, "troll_staff");
        RECIPE_CLONING_CANE = new NamespacedKey(plugin, "cloning_cane");
        MOB_KILLS = new NamespacedKey(plugin, "mob_kills");
        SCEPTER_MODE = new NamespacedKey(plugin, "scepter_mode");
        WARDEN_WAND_MODE = new NamespacedKey(plugin, "warden_wand_mode");
        ENDER_WAND_MODE = new NamespacedKey(plugin, "ender_wand_mode");
        CLONING_MODE = new NamespacedKey(plugin, "cloning_mode");
        DRIPSTONE = new NamespacedKey(plugin, "sword_dripstone");
        BLAZE_FIREBALL = new NamespacedKey(plugin, "blaze_fireball");
        WAND_MODE = new NamespacedKey(plugin, "wand_mode");
        WAND_FIREBALL = new NamespacedKey(plugin, "wand_fireball");
        WAND_TNT = new NamespacedKey(plugin, "wand_tnt");
        SWORD_ATTACK_DAMAGE = new NamespacedKey(plugin, "sword_attack_damage");
        SWORD_ATTACK_SPEED = new NamespacedKey(plugin, "sword_attack_speed");
        BUCKET_WATER = new NamespacedKey(plugin, "bucket_water");
        BUCKET_LAVA = new NamespacedKey(plugin, "bucket_lava");
        BUCKET_MODE = new NamespacedKey(plugin, "bucket_mode");
    }
}
