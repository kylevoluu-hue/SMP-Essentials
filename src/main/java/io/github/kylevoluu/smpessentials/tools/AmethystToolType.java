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
            });

    private final String markerValue;
    private final Material baseMaterial;
    private final String displayName;
    private final String[] lore;

    AmethystToolType(String markerValue, Material baseMaterial, String displayName, String[] lore) {
        this.markerValue = markerValue;
        this.baseMaterial = baseMaterial;
        this.displayName = displayName;
        this.lore = lore;
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
            default -> null;
        };
    }
}
