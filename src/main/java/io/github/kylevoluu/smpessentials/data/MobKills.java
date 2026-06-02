package io.github.kylevoluu.smpessentials.data;

import io.github.kylevoluu.smpessentials.keys.Keys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Tracks how many mobs each player has killed, stored in the player's
 * PersistentDataContainer (so it survives restarts). The Cloning Cane scales its
 * clone count with this value.
 */
public final class MobKills implements Listener {

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        int current = get(killer);
        killer.getPersistentDataContainer().set(Keys.MOB_KILLS, PersistentDataType.INTEGER, current + 1);
    }

    public static int get(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.MOB_KILLS, PersistentDataType.INTEGER, 0);
    }
}
