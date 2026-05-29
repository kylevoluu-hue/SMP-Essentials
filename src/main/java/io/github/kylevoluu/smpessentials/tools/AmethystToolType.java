package io.github.kylevoluu.smpessentials.tools;

import org.bukkit.Material;

import java.util.Locale;

/**
 * The two custom Amethyst tools. Each carries the data needed to build the item
 * (base material, display name, lore) and the marker value stored in the item's
 * PersistentDataContainer so listeners can recognise it later.
 */
public enum AmethystToolType {

    PICKAXE("amethyst_pickaxe", Material.DIAMOND_PICKAXE, "Amethyst Pickaxe",
            new String[]{
                    "&7Mines a &d3x3&7 area at once.",
                    "&7Works with Fortune & Silk Touch."
            }),

    AXE("amethyst_axe", Material.DIAMOND_AXE, "Amethyst Axe",
            new String[]{
                    "&7Fells an entire tree in one chop.",
                    "&7Works with Fortune & Silk Touch."
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
            default -> null;
        };
    }
}
