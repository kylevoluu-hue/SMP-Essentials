package io.github.kylevoluu.smpessentials.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Tiny helper for multi-mode ability items: stores the selected mode string in the
 * item's PersistentDataContainer under a given key, and cycles through an order.
 */
public final class ModeStore {

    private ModeStore() {
    }

    public static String get(ItemStack item, NamespacedKey key, String fallback) {
        if (item == null || !item.hasItemMeta()) {
            return fallback;
        }
        String mode = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return mode == null ? fallback : mode;
    }

    /** Advance to the next mode in {@code order}, persist it, and return it. */
    public static String cycle(ItemStack item, NamespacedKey key, String[] order) {
        String current = get(item, key, order[0]);
        int index = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(current)) {
                index = i;
                break;
            }
        }
        String next = order[(index + 1) % order.length];
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, next);
        item.setItemMeta(meta);
        return next;
    }
}
