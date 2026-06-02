package io.github.kylevoluu.smpessentials.tools;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads/writes the Blaze Wand's selected mode (in the item's
 * PersistentDataContainer) and rebuilds its lore display.
 */
public final class BlazeWand {

    public static final String SUMMON = "summon";
    public static final String RANGE = "range";
    public static final String BOW = "bow";

    private static final String[] ORDER = {SUMMON, RANGE, BOW};

    private BlazeWand() {
    }

    public static String getMode(ItemStack item) {
        String mode = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.WAND_MODE, PersistentDataType.STRING);
        return mode == null ? SUMMON : mode;
    }

    /** Advance to the next mode and return it. */
    public static String cycleMode(ItemStack item) {
        String current = getMode(item);
        int index = 0;
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(current)) {
                index = i;
                break;
            }
        }
        String next = ORDER[(index + 1) % ORDER.length];
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.WAND_MODE, PersistentDataType.STRING, next);
        item.setItemMeta(meta);
        refreshDisplay(item);
        return next;
    }

    public static void initialize(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Keys.WAND_MODE, PersistentDataType.STRING)) {
            meta.getPersistentDataContainer().set(Keys.WAND_MODE, PersistentDataType.STRING, SUMMON);
        }
        item.setItemMeta(meta);
        refreshDisplay(item);
    }

    public static void refreshDisplay(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String mode = getMode(item);
        List<Component> lore = new ArrayList<>();
        lore.add(line("&7Right-click to use. Sneak + right-click to switch mode."));
        lore.add(Component.empty());
        lore.add(line("&6Mode: &e" + mode.toUpperCase(Locale.ROOT)));
        lore.add(line(description(mode)));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static String description(String mode) {
        return switch (mode) {
            case RANGE -> "&7Fires a volley of fireballs.";
            case BOW -> "&7Lobs explosive, fiery TNT.";
            default -> "&7Summons a blaze to fight for you.";
        };
    }

    private static Component line(String legacy) {
        return Text.of(legacy).decoration(TextDecoration.ITALIC, false);
    }
}
