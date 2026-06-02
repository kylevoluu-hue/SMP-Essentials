package io.github.kylevoluu.smpessentials.pack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Optionally pushes the SMP Essentials resource pack to players when they join,
 * so admins don't have to touch server.properties. Sending on join (after the
 * configuration phase) avoids the server.properties "error during configuration"
 * pitfall. The pack is still downloaded by the client from the configured URL.
 */
public final class ResourcePackListener implements Listener {

    private final Plugin plugin;

    public ResourcePackListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.push-on-join", false)) {
            return;
        }
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        byte[] hash = parseSha1(plugin.getConfig().getString("resource-pack.sha1", ""));
        boolean force = plugin.getConfig().getBoolean("resource-pack.required", false);
        try {
            if (hash != null) {
                player.setResourcePack(url, hash, force);
            } else {
                player.setResourcePack(url);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not send resource pack: " + t.getMessage());
        }
    }

    /** Parse a 40-char hex SHA-1 into 20 bytes, or null if missing/invalid. */
    private byte[] parseSha1(String sha1) {
        if (sha1 == null) {
            return null;
        }
        String s = sha1.trim();
        if (s.length() != 40) {
            return null;
        }
        try {
            byte[] bytes = new byte[20];
            for (int i = 0; i < 20; i++) {
                bytes[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
