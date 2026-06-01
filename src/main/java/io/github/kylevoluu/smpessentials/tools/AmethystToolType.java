package io.github.kylevoluu.smpessentials.tools;

import org.bukkit.Material;

import java.util.Locale;

/**
 * The two custom Amethyst tools. Each carries the data needed to build the item
 * (base material, display name, lore) and the marker value stored in the item's
 * PersistentDataContainer so listeners can recognise it later.
 */
public enum AmethystToolType {

    PICKAXE("amethyst_pickaxe", Material.NETHERITE_PICKAXE, "Amethyst Pickaxe",
            new String[]{
                    "&7Mines a &d3x3&7 area at once.",
                    "&7Works with Fortune & Silk Touch."
            }),

    AXE("amethyst_axe", Material.NETHERITE_AXE, "Amethyst Axe",
            new String[]{
                    "&7Fells an entire tree in one chop.",
                    "&7Works with Fortune & Silk Touch."
            }),

    SHOVEL("amethyst_shovel", Material.NETHERITE_SHOVEL, "Amethyst Shovel",
            new String[]{
                    "&7Digs a &d5x5&7 area at once.",
                    "&7Dirt, sand, gravel and the like."
            }),

    SWORD("amethyst_sword", Material.NETHERITE_SWORD, "Amethyst Sword",
            new String[]{
                    "&7Base damage &d10&7.",
                    "&7Drops dripstone onto your target."
            }),

    BUCKET("amethyst_bucket", Material.BUCKET, "Amethyst Bucket",
            new String[]{
                    "&7Drains whole pools of water/lava.",
                    "&7Sneak + right-click to switch what you pour."
            }),

    BLAZE_SWORD("blaze_sword", Material.GOLDEN_SWORD, "Blaze Sword",
            new String[]{
                    "&6Base damage &c12&6.",
                    "&6Ignites and fireballs your target."
            }),

    BLAZE_WAND("blaze_wand", Material.BLAZE_ROD, "Blaze Wand",
            new String[]{
                    "&6A multi-mode blaze wand.",
                    "&7Sneak + right-click to switch mode."
            }),

    STORM_ROD("storm_rod", Material.LIGHTNING_ROD, "Storm Rod",
            new String[]{
                    "&eRight-click to call down lightning.",
                    "&eSmites whatever you strike in melee."
            }),

    // --- Ability items (custom model data + 1.5x netherite durability + Mending) ---

    WARDEN_CROSSBOW("warden_crossbow", Material.CROSSBOW, "Warden Crossbow",
            new String[]{
                    "&3Fires a &bsonic boom&3.",
                    "&8With Multishot: three booms."
            }, 1, 3047),

    STORM_SCEPTER("storm_scepter", Material.CARROT_ON_A_STICK, "Storm Scepter",
            new String[]{
                    "&fModes: lightning bow, sunken, summon.",
                    "&7Sneak + right-click to switch mode."
            }, 2, 3047),

    WARDEN_WAND("warden_wand", Material.CARROT_ON_A_STICK, "Warden Wand",
            new String[]{
                    "&3Modes: summon, range, boss.",
                    "&7Sneak + right-click to switch mode."
            }, 3, 3047),

    ENDER_WAND("ender_wand", Material.CARROT_ON_A_STICK, "Ender Wand",
            new String[]{
                    "&5Modes: pearls, ranged, summon.",
                    "&7Sneak + right-click to switch mode."
            }, 4, 3047),

    TROLL_STAFF("troll_staff", Material.CARROT_ON_A_STICK, "Troll Staff",
            new String[]{
                    "&dRight-click for an eerie scare.",
                    "&7Plays random spooky sounds nearby."
            }, 5, 3047),

    CLONING_CANE("cloning_cane", Material.CARROT_ON_A_STICK, "Cloning Cane",
            new String[]{
                    "&bModes: confusion, explode.",
                    "&7Charge grows with mob kills."
            }, 6, 3047);

    private final String markerValue;
    private final Material baseMaterial;
    private final String displayName;
    private final String[] lore;
    private final int customModelData;
    private final int maxDurability;

    AmethystToolType(String markerValue, Material baseMaterial, String displayName, String[] lore) {
        this(markerValue, baseMaterial, displayName, lore, 0, 0);
    }

    AmethystToolType(String markerValue, Material baseMaterial, String displayName, String[] lore,
                     int customModelData, int maxDurability) {
        this.markerValue = markerValue;
        this.baseMaterial = baseMaterial;
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.maxDurability = maxDurability;
    }

    /** Custom model data for the resource pack (0 = none). */
    public int customModelData() {
        return customModelData;
    }

    /** Overridden max durability (0 = keep the material default). */
    public int maxDurability() {
        return maxDurability;
    }

    public String markerValue() {
        return markerValue;
    }

    public Material baseMaterial() {
        return baseMaterial;
    }

    public String displayName() {
        return displayName;
    }

    public String[] loreLines() {
        return lore;
    }

    /** Match a user-supplied argument (e.g. "pickaxe", "axe") to a type. */
    public static AmethystToolType fromArgument(String arg) {
        if (arg == null) {
            return null;
        }
        return switch (arg.toLowerCase(Locale.ROOT)) {
            case "pickaxe", "pick", "pickaxe3x3" -> PICKAXE;
            case "axe", "treeaxe", "treecapitator" -> AXE;
            case "shovel", "spade" -> SHOVEL;
            case "sword" -> SWORD;
            case "bucket" -> BUCKET;
            case "blaze", "blazesword", "blaze_sword" -> BLAZE_SWORD;
            case "wand", "blazewand", "blaze_wand" -> BLAZE_WAND;
            case "storm", "stormrod", "storm_rod", "rod" -> STORM_ROD;
            case "wardencrossbow", "warden_crossbow", "crossbow" -> WARDEN_CROSSBOW;
            case "stormscepter", "storm_scepter", "scepter" -> STORM_SCEPTER;
            case "wardenwand", "warden_wand", "warden" -> WARDEN_WAND;
            case "enderwand", "ender_wand", "ender" -> ENDER_WAND;
            case "trollstaff", "troll_staff", "troll" -> TROLL_STAFF;
            case "cloningcane", "cloning_cane", "cloning", "cane" -> CLONING_CANE;
            default -> null;
        };
    }
}
