package io.github.kylevoluu.smpessentials.treefeller;

import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.tools.ToolDamage;
import io.github.kylevoluu.smpessentials.tools.ToolSoundListener;
import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Makes the Amethyst Axe fell an entire tree when a single log is broken.
 *
 * <p>Connected logs are found with a bounded flood-fill over the 26-block
 * neighbourhood. Like the area miner, each extra block fires its own
 * {@link BlockBreakEvent} so protection plugins can veto it, and a per-player
 * guard prevents recursion.</p>
 */
public final class TreeFellerListener implements Listener {

    private final Plugin plugin;
    private final Set<UUID> processing = new HashSet<>();

    public TreeFellerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("amethyst-tools.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (processing.contains(player.getUniqueId())) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(tool, AmethystToolType.AXE)) {
            return;
        }

        Block origin = event.getBlock();
        if (!Tag.LOGS.isTagged(origin.getType())) {
            return; // Only trigger when chopping a log.
        }

        int maxLogs = plugin.getConfig().getInt("amethyst-tools.axe.max-logs", 256);
        boolean damageTool = plugin.getConfig().getBoolean("amethyst-tools.axe.damage-tool", false);
        boolean breakLeaves = plugin.getConfig().getBoolean("amethyst-tools.axe.break-leaves", false);

        if (ToolSoundListener.enabled(plugin)) {
            ToolSoundListener.playChime(player);
        }

        processing.add(player.getUniqueId());
        try {
            for (Block log : collectTree(origin, maxLogs)) {
                if (fellBlock(player, tool, log) && damageTool && !ToolDamage.damageMainHand(player)) {
                    return; // Tool broke; stop felling.
                }
            }
            if (breakLeaves) {
                for (Block leaf : collectLeaves(origin, maxLogs)) {
                    fellBlock(player, tool, leaf);
                }
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    /** Flood-fill connected logs (excluding the origin, which the original event breaks). */
    private Set<Block> collectTree(Block origin, int maxLogs) {
        Set<Block> logs = new HashSet<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && logs.size() < maxLogs) {
            Block current = queue.poll();
            for (Block neighbour : neighbours(current)) {
                if (!visited.add(neighbour)) {
                    continue;
                }
                if (Tag.LOGS.isTagged(neighbour.getType())) {
                    logs.add(neighbour);
                    queue.add(neighbour);
                }
            }
        }
        return logs;
    }

    /** Leaves adjacent to the felled tree, used only when break-leaves is enabled. */
    private Set<Block> collectLeaves(Block origin, int maxLogs) {
        Set<Block> leaves = new HashSet<>();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 6; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (leaves.size() >= maxLogs) {
                        return leaves;
                    }
                    Block b = origin.getRelative(dx, dy, dz);
                    if (Tag.LEAVES.isTagged(b.getType())) {
                        leaves.add(b);
                    }
                }
            }
        }
        return leaves;
    }

    /**
     * Fire a break event so protection plugins can veto it, then break the block.
     *
     * @return {@code true} if the block was actually broken (event not cancelled).
     */
    private boolean fellBlock(Player player, ItemStack tool, Block block) {
        BlockBreakEvent simulated = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(simulated);
        if (simulated.isCancelled()) {
            return false;
        }
        block.breakNaturally(tool);
        return true;
    }

    private Iterable<Block> neighbours(Block block) {
        Set<Block> result = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    result.add(block.getRelative(dx, dy, dz));
                }
            }
        }
        return result;
    }
}
