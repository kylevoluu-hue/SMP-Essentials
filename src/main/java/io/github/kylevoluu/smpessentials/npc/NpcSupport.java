package io.github.kylevoluu.smpessentials.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Thin wrapper around the (optional) Citizens API for spawning full-body player
 * NPCs — used for the Cloning Cane clones and the Troll Staff's Herobrine.
 *
 * <p>This class references Citizens types, so it must only be touched after
 * {@link #available()} returns true (which itself only uses the Bukkit API).
 * Callers gate on that, so the class is never loaded when Citizens is absent.</p>
 */
public final class NpcSupport {

    private NpcSupport() {
    }

    /** Bukkit-only check; safe to call without Citizens on the classpath. */
    public static boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }

    /** Spawn a player-type NPC wearing {@code owner}'s skin; returns its entity (or null). */
    public static Entity spawnClone(Player owner, Location location) {
        return spawn(location, owner.getName(), owner.getName());
    }

    /** Spawn a player-type NPC with the given name and skin (skin = a username), or null on failure. */
    public static Entity spawn(Location location, String name, String skinName) {
        try {
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
            npc.setProtected(true);
            if (skinName != null && !skinName.isBlank()) {
                npc.getOrAddTrait(SkinTrait.class).setSkinName(skinName);
            }
            npc.spawn(location);
            return npc.getEntity();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Make an NPC entity path toward a target (Citizens handles movement + jumping). */
    public static void navigateTo(Entity npcEntity, Entity target) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(npcEntity);
        if (npc != null) {
            npc.getNavigator().setTarget(target, true);
        }
    }

    /** Destroy the NPC backing this entity, if any. */
    public static void despawn(Entity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(npcEntity);
        if (npc != null) {
            npc.destroy();
        }
    }
}
