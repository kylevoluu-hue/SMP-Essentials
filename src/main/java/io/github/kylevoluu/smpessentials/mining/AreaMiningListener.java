package io.github.kylevoluu.smpessentials.mining;

import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import io.github.kylevoluu.smpessentials.tools.ToolSoundListener;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Makes the Amethyst Pickaxe mine a 3x3 area in the plane facing the player.
 *
 * <p>Each extra block fires its own {@link BlockBreakEvent} so land-protection
 * plugins (WorldGuard, GriefPrevention, …) can veto it. A per-player guard set
 * prevents our simulated events from recursing back into this handler.</p>
 */
public final class AreaMiningListener implements Listener {

    private final Plugin plugin;
    private final Set<UUID> processing = new HashSet<>();

    public AreaMiningListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("amethyst-tools.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (processing.contains(player.getUniqueId())) {
            return; // This is one of our own simulated breaks.
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return; // Vanilla instant-break already covers creative.
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(tool, AmethystToolType.PICKAXE)) {
            return;
        }

        Block origin = event.getBlock();
        BlockFace face = resolveFace(player);
        boolean damageTool = plugin.getConfig().getBoolean("amethyst-tools.pickaxe.damage-tool", true);

        if (ToolSoundListener.enabled(plugin)) {
            ToolSoundListener.playChime(player);
        }

        processing.add(player.getUniqueId());
        try {
            for (Block target : planeAround(origin, face)) {
                if (target.equals(origin)) {
                    continue; // The origin is handled by the original event.
                }
                if (!isMineable(target, tool)) {
                    continue;
                }
                // Let protection plugins decide on each block.
                BlockBreakEvent simulated = new BlockBreakEvent(target, player);
                plugin.getServer().getPluginManager().callEvent(simulated);
                if (simulated.isCancelled()) {
                    continue;
                }

                target.breakNaturally(tool);

                if (damageTool && !ToolDamage.damageMainHand(player)) {
                    break; // Tool broke; stop mining.
                }
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    /** Determine the block face the player is looking at, defaulting to UP. */
    private BlockFace resolveFace(Player player) {
        RayTraceResult result = player.rayTraceBlocks(6.0);
        if (result != null && result.getHitBlockFace() != null) {
            return result.getHitBlockFace();
        }
        return BlockFace.UP;
    }

    /** The 3x3 set of blocks in the plane perpendicular to {@code face}, centred on {@code origin}. */
    private Set<Block> planeAround(Block origin, BlockFace face) {
        Set<Block> blocks = new HashSet<>();
        switch (face) {
            case UP, DOWN -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        blocks.add(origin.getRelative(dx, 0, dz));
                    }
                }
            }
            case NORTH, SOUTH -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        blocks.add(origin.getRelative(dx, dy, 0));
                    }
                }
            }
            default -> { // EAST, WEST and any diagonal fallback
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        blocks.add(origin.getRelative(0, dy, dz));
                    }
                }
            }
        }
        return blocks;
    }

    /** Only break solid, non-air, tool-appropriate, breakable blocks. */
    private boolean isMineable(Block block, ItemStack tool) {
        if (block.isEmpty() || block.isLiquid()) {
            return false;
        }
        if (block.getType().getHardness() < 0) {
            return false; // Bedrock, barrier, etc.
        }
        return block.isPreferredTool(tool);
    }
}
