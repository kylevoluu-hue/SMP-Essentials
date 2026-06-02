package io.github.kylevoluu.smpessentials.tools;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads/writes the Amethyst Bucket's stored water/lava amounts and selected pour
 * mode (all in the item's PersistentDataContainer) and rebuilds its lore display.
 */
public final class AmethystBucket {

    public static final String WATER = "water";
    public static final String LAVA = "lava";

    private AmethystBucket() {
    }

    public static int getWater(ItemStack item) {
        return get(item, Keys.BUCKET_WATER);
    }

    public static int getLava(ItemStack item) {
        return get(item, Keys.BUCKET_LAVA);
    }

    public static String getMode(ItemStack item) {
        String mode = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BUCKET_MODE, PersistentDataType.STRING);
        return mode == null ? WATER : mode;
    }

    public static void setWater(ItemStack item, int value) {
        set(item, Keys.BUCKET_WATER, Math.max(0, value));
    }

    public static void setLava(ItemStack item, int value) {
        set(item, Keys.BUCKET_LAVA, Math.max(0, value));
    }

    /** Toggle the pour mode between water and lava; returns the new mode. */
    public static String toggleMode(ItemStack item) {
        String next = getMode(item).equals(WATER) ? LAVA : WATER;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.BUCKET_MODE, PersistentDataType.STRING, next);
        item.setItemMeta(meta);
        refreshDisplay(item);
        return next;
    }

    /** Ensure the PDC fields exist and the display reflects them. */
    public static void initialize(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(Keys.BUCKET_WATER, PersistentDataType.INTEGER)) {
            pdc.set(Keys.BUCKET_WATER, PersistentDataType.INTEGER, 0);
        }
        if (!pdc.has(Keys.BUCKET_LAVA, PersistentDataType.INTEGER)) {
            pdc.set(Keys.BUCKET_LAVA, PersistentDataType.INTEGER, 0);
        }
        if (!pdc.has(Keys.BUCKET_MODE, PersistentDataType.STRING)) {
            pdc.set(Keys.BUCKET_MODE, PersistentDataType.STRING, WATER);
        }
        item.setItemMeta(meta);
        refreshDisplay(item);
    }

    /** Rebuild the bucket's lore to show stored amounts and the selected mode. */
    public static void refreshDisplay(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(line("&7Right-click a pool to drain it."));
        lore.add(line("&7Sneak + right-click to switch pour mode."));
        lore.add(Component.empty());
        lore.add(line("&bWater: &f" + getWater(item)));
        lore.add(line("&cLava: &f" + getLava(item)));
        lore.add(line("&7Pouring: &e" + getMode(item).toUpperCase(Locale.ROOT)));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static Component line(String legacy) {
        return Text.of(legacy).decoration(TextDecoration.ITALIC, false);
    }

    private static int get(ItemStack item, NamespacedKey key) {
        Integer value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    private static void set(ItemStack item, NamespacedKey key, int value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }
}
