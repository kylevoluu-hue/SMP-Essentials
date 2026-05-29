package io.github.kylevoluu.smpessentials.tools;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies Unbreaking-aware durability damage to a player's main-hand tool, the
 * same way vanilla does when breaking a block.
 */
public final class ToolDamage {

    private ToolDamage() {
    }

    private static Enchantment unbreaking() {
        // Enchantments are registry-backed on Paper 26.1 (the old static fields
        // are deprecated). Resolve UNBREAKING from the registry.
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft("unbreaking"));
    }

    /**
     * Apply one point of durability damage to the player's main-hand tool,
     * respecting Unbreaking and the player's game mode.
     *
     * @return {@code true} if the tool is still usable, {@code false} if it just broke.
     */
    public static boolean damageMainHand(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().getMaxDurability() <= 0) {
            return true;
        }
        if (!(tool.getItemMeta() instanceof Damageable damageable)) {
            return true;
        }

        // Vanilla Unbreaking: each use has a 1/(level+1) chance of actually
        // consuming durability.
        Enchantment unbreaking = unbreaking();
        int level = unbreaking == null ? 0 : tool.getEnchantmentLevel(unbreaking);
        if (level > 0 && ThreadLocalRandom.current().nextInt(level + 1) != 0) {
            return true; // Unbreaking saved the durability point.
        }

        int newDamage = damageable.getDamage() + 1;
        int max = tool.getType().getMaxDurability();
        if (newDamage >= max) {
            // Tool broke: remove it and play the vanilla break effect.
            player.getInventory().setItemInMainHand(null);
            player.getWorld().playSound(player.getLocation(), "minecraft:entity.item.break", 1.0f, 1.0f);
            player.updateInventory();
            return false;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
        player.getInventory().setItemInMainHand(tool);
        return true;
    }
}
