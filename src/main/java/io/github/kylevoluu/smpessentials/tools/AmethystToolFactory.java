package io.github.kylevoluu.smpessentials.tools;

import io.github.kylevoluu.smpessentials.keys.Keys;
import io.github.kylevoluu.smpessentials.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds Amethyst tool {@link ItemStack}s and recognises them again later via
 * their PersistentDataContainer marker.
 */
public final class AmethystToolFactory {

    private AmethystToolFactory() {
    }

    /** Create a fresh Amethyst tool item of the given type. */
    public static ItemStack create(AmethystToolType type) {
        ItemStack item = new ItemStack(type.baseMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.displayName())
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : type.loreLines()) {
            lore.add(Text.of(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        // Glint without needing a real enchantment. On Paper 26.1 this takes a
        // boxed Boolean (true = always glint). The tool is still fully enchantable.
        meta.setEnchantmentGlintOverride(Boolean.TRUE);

        // Mark the item so listeners/commands can identify it regardless of name edits.
        meta.getPersistentDataContainer().set(Keys.TOOL, PersistentDataType.STRING, type.markerValue());

        item.setItemMeta(meta);
        return item;
    }

    /** Return the Amethyst tool type of an item, or {@code null} if it is not one. */
    public static AmethystToolType typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String value = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.TOOL, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        for (AmethystToolType type : AmethystToolType.values()) {
            if (type.markerValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /** Convenience check that an item is the specific Amethyst tool type. */
    public static boolean is(ItemStack item, AmethystToolType type) {
        return typeOf(item) == type;
    }
}
