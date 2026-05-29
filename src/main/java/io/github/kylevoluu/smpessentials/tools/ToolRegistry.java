package io.github.kylevoluu.smpessentials.tools;

import io.github.kylevoluu.smpessentials.keys.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers and unregisters the crafting recipes for the Amethyst tools.
 *
 * <p>The recipes mirror the vanilla diamond tool layouts, but the three "head"
 * pieces are replaced with <b>1 amethyst shard + 2 netherite ingots</b>.</p>
 */
public final class ToolRegistry {

    private ToolRegistry() {
    }

    /** Register both recipes. Safe to call on enable / reload. */
    public static void register() {
        // Pickaxe:  N A N      (N = netherite ingot, A = amethyst shard, S = stick)
        //             S
        //             S
        ShapedRecipe pickaxe = new ShapedRecipe(Keys.RECIPE_PICKAXE,
                AmethystToolFactory.create(AmethystToolType.PICKAXE));
        pickaxe.shape("NAN", " S ", " S ");
        pickaxe.setIngredient('N', Material.NETHERITE_INGOT);
        pickaxe.setIngredient('A', Material.AMETHYST_SHARD);
        pickaxe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(pickaxe);

        // Axe:  A N
        //       N S
        //         S
        ShapedRecipe axe = new ShapedRecipe(Keys.RECIPE_AXE,
                AmethystToolFactory.create(AmethystToolType.AXE));
        axe.shape("AN", "NS", " S");
        axe.setIngredient('A', Material.AMETHYST_SHARD);
        axe.setIngredient('N', Material.NETHERITE_INGOT);
        axe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(axe);
    }

    /** Remove both recipes. Called on disable and before a reload re-registers. */
    public static void unregister() {
        removeQuietly(Keys.RECIPE_PICKAXE);
        removeQuietly(Keys.RECIPE_AXE);
    }

    private static void removeQuietly(NamespacedKey key) {
        try {
            Bukkit.removeRecipe(key);
        } catch (Exception ignored) {
            // Recipe may not be registered yet; ignore.
        }
    }
}
