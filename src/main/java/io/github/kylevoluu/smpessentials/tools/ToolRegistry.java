package io.github.kylevoluu.smpessentials.tools;

import io.github.kylevoluu.smpessentials.keys.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers and unregisters the crafting recipes for the Amethyst tools.
 *
 * <p>Every tool is intentionally expensive: each recipe costs
 * <b>1 amethyst block + 2 netherite blocks</b> (N = netherite block,
 * A = amethyst block, S = stick).</p>
 */
public final class ToolRegistry {

    private ToolRegistry() {
    }

    /** Register all tool recipes. Safe to call on enable / reload. */
    public static void register() {
        // Pickaxe:  N A N
        //             S
        //             S
        ShapedRecipe pickaxe = new ShapedRecipe(Keys.RECIPE_PICKAXE,
                AmethystToolFactory.create(AmethystToolType.PICKAXE));
        pickaxe.shape("NAN", " S ", " S ");
        addBlocks(pickaxe);
        pickaxe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(pickaxe);

        // Axe:  A N
        //       N S
        //         S
        ShapedRecipe axe = new ShapedRecipe(Keys.RECIPE_AXE,
                AmethystToolFactory.create(AmethystToolType.AXE));
        axe.shape("AN", "NS", " S");
        addBlocks(axe);
        axe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(axe);

        // Shovel:  A
        //          N
        //          N
        ShapedRecipe shovel = new ShapedRecipe(Keys.RECIPE_SHOVEL,
                AmethystToolFactory.create(AmethystToolType.SHOVEL));
        shovel.shape("A", "N", "N");
        addBlocks(shovel);
        Bukkit.addRecipe(shovel);

        // Sword:  A
        //         N
        //         N
        ShapedRecipe sword = new ShapedRecipe(Keys.RECIPE_SWORD,
                AmethystToolFactory.create(AmethystToolType.SWORD));
        sword.shape("A", "N", "N");
        addBlocks(sword);
        Bukkit.addRecipe(sword);

        // Bucket:  N N
        //           A
        ShapedRecipe bucket = new ShapedRecipe(Keys.RECIPE_BUCKET,
                AmethystToolFactory.create(AmethystToolType.BUCKET));
        bucket.shape("N N", " A ");
        addBlocks(bucket);
        Bukkit.addRecipe(bucket);

        // Blaze Sword:  S S   (S = nether star, M = magma block, B = blaze rod)
        //                M
        //                B
        ShapedRecipe blazeSword = new ShapedRecipe(Keys.RECIPE_BLAZE_SWORD,
                AmethystToolFactory.create(AmethystToolType.BLAZE_SWORD));
        blazeSword.shape("S S", " M ", " B ");
        blazeSword.setIngredient('S', Material.NETHER_STAR);
        blazeSword.setIngredient('M', Material.MAGMA_BLOCK);
        blazeSword.setIngredient('B', Material.BLAZE_ROD);
        Bukkit.addRecipe(blazeSword);

        // Blaze Wand:  R N R   (R = blaze rod, N = nether star, M = magma block)
        //              R M R
        //               R
        ShapedRecipe blazeWand = new ShapedRecipe(Keys.RECIPE_BLAZE_WAND,
                AmethystToolFactory.create(AmethystToolType.BLAZE_WAND));
        blazeWand.shape("RNR", "RMR", " R ");
        blazeWand.setIngredient('R', Material.BLAZE_ROD);
        blazeWand.setIngredient('N', Material.NETHER_STAR);
        blazeWand.setIngredient('M', Material.MAGMA_BLOCK);
        Bukkit.addRecipe(blazeWand);

        // Storm Rod:  N    (N = nether star, C = copper block, L = lightning rod)
        //            CLC
        //             C
        ShapedRecipe stormRod = new ShapedRecipe(Keys.RECIPE_STORM_ROD,
                AmethystToolFactory.create(AmethystToolType.STORM_ROD));
        stormRod.shape(" N ", "CLC", " C ");
        stormRod.setIngredient('N', Material.NETHER_STAR);
        stormRod.setIngredient('C', Material.COPPER_BLOCK);
        stormRod.setIngredient('L', Material.LIGHTNING_ROD);
        Bukkit.addRecipe(stormRod);

        // Troll Staff:  N    (N = note block, S = stick, K = sculk sensor)
        //               S
        //               K
        ShapedRecipe troll = new ShapedRecipe(Keys.RECIPE_TROLL_STAFF,
                AmethystToolFactory.create(AmethystToolType.TROLL_STAFF));
        troll.shape(" N ", " S ", " K ");
        troll.setIngredient('N', Material.NOTE_BLOCK);
        troll.setIngredient('S', Material.STICK);
        troll.setIngredient('K', Material.SCULK_SENSOR);
        Bukkit.addRecipe(troll);
    }

    /** Bind 'A' to an amethyst block and 'N' to a netherite block. */
    private static void addBlocks(ShapedRecipe recipe) {
        recipe.setIngredient('A', Material.AMETHYST_BLOCK);
        recipe.setIngredient('N', Material.NETHERITE_BLOCK);
    }

    /** Remove all recipes. Called on disable and before a reload re-registers. */
    public static void unregister() {
        removeQuietly(Keys.RECIPE_PICKAXE);
        removeQuietly(Keys.RECIPE_AXE);
        removeQuietly(Keys.RECIPE_SHOVEL);
        removeQuietly(Keys.RECIPE_SWORD);
        removeQuietly(Keys.RECIPE_BUCKET);
        removeQuietly(Keys.RECIPE_BLAZE_SWORD);
        removeQuietly(Keys.RECIPE_BLAZE_WAND);
        removeQuietly(Keys.RECIPE_STORM_ROD);
        removeQuietly(Keys.RECIPE_TROLL_STAFF);
    }

    private static void removeQuietly(NamespacedKey key) {
        try {
            Bukkit.removeRecipe(key);
        } catch (Exception ignored) {
            // Recipe may not be registered yet; ignore.
        }
    }
}
