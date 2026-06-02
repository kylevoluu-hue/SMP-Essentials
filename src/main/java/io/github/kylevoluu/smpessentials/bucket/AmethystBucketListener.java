package io.github.kylevoluu.smpessentials.bucket;

import io.github.kylevoluu.smpessentials.tools.AmethystBucket;
import io.github.kylevoluu.smpessentials.tools.AmethystToolFactory;
import io.github.kylevoluu.smpessentials.tools.AmethystToolType;
import io.github.kylevoluu.smpessentials.util.Messages;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The Amethyst Bucket: right-click a water/lava pool to drain the whole connected
 * body into unlimited storage (auto-detecting which fluid), sneak + right-click to
 * switch which stored fluid you pour, and right-click a block to place a source of
 * the selected fluid.
 */
public final class AmethystBucketListener implements Listener {

    private static final BlockFace[] NEIGHBOURS = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private final Plugin plugin;
    private final Messages messages;

    public AmethystBucketListener(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("amethyst-tools.enabled", true)) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        if (!AmethystToolFactory.is(item, AmethystToolType.BUCKET)) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        event.setCancelled(true); // We fully control the Amethyst Bucket.

        // Work on the live hand item and write it back, so changes always persist
        // (PlayerInteractEvent#getItem can be a copy on some implementations).
        ItemStack bucket = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!AmethystToolFactory.is(bucket, AmethystToolType.BUCKET)) {
            return;
        }

        if (player.isSneaking()) {
            // Sneak + right-click toggles which stored fluid is poured.
            String mode = AmethystBucket.toggleMode(bucket);
            player.sendMessage(messages.prefixed("bucket-mode-switched", "mode", mode.toUpperCase(Locale.ROOT)));
        } else {
            RayTraceResult fluidHit = player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS);
            if (fluidHit != null && fluidHit.getHitBlock() != null && fluidHit.getHitBlock().isLiquid()) {
                // Aiming at a fluid: drain the whole connected pool.
                drain(player, bucket, fluidHit.getHitBlock());
            } else if (event.getClickedBlock() != null) {
                // Otherwise place the selected fluid against the clicked block.
                place(player, bucket, event.getClickedBlock().getRelative(event.getBlockFace()));
            } else {
                return;
            }
        }

        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(bucket);
        } else {
            player.getInventory().setItemInMainHand(bucket);
        }
        player.updateInventory();
    }

    private void drain(Player player, ItemStack item, Block start) {
        Material fluid = start.getType();
        if (fluid != Material.WATER && fluid != Material.LAVA) {
            return;
        }
        int cap = Math.max(1, plugin.getConfig().getInt("amethyst-tools.bucket.max-drain-blocks", 1024));

        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int drained = 0;

        while (!queue.isEmpty() && drained < cap) {
            Block current = queue.poll();
            if (current.getType() != fluid) {
                continue;
            }
            current.setType(Material.AIR, false);
            drained++;
            for (BlockFace face : NEIGHBOURS) {
                Block neighbour = current.getRelative(face);
                if (visited.add(neighbour) && neighbour.getType() == fluid) {
                    queue.add(neighbour);
                }
            }
        }

        if (fluid == Material.WATER) {
            AmethystBucket.setWater(item, AmethystBucket.getWater(item) + drained);
        } else {
            AmethystBucket.setLava(item, AmethystBucket.getLava(item) + drained);
        }
        AmethystBucket.refreshDisplay(item);
        player.sendMessage(messages.prefixed("bucket-collected",
                "amount", String.valueOf(drained),
                "fluid", fluid == Material.WATER ? "water" : "lava"));
    }

    private void place(Player player, ItemStack item, Block target) {
        String mode = AmethystBucket.getMode(item);
        boolean lava = mode.equals(AmethystBucket.LAVA);
        int stored = lava ? AmethystBucket.getLava(item) : AmethystBucket.getWater(item);
        if (stored <= 0) {
            player.sendMessage(messages.prefixed("bucket-empty", "fluid", mode));
            return;
        }
        if (!target.isEmpty() && !target.isReplaceable()) {
            return;
        }
        target.setType(lava ? Material.LAVA : Material.WATER);
        if (lava) {
            AmethystBucket.setLava(item, stored - 1);
        } else {
            AmethystBucket.setWater(item, stored - 1);
        }
        AmethystBucket.refreshDisplay(item);
        player.sendMessage(messages.prefixed("bucket-placed", "fluid", mode));
    }
}
