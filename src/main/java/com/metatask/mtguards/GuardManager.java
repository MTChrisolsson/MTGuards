package com.metatask.mtguards;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuardManager {

    private final JavaPlugin plugin;
    private final NPCRegistry registry;
    private final Map<UUID, NPC> guards = new HashMap<>();

    public GuardManager(JavaPlugin plugin, NPCRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public NPC createGuard(Player creator, String name, Integer radiusOverride) {
        FileConfiguration config = plugin.getConfig();
        int defaultRadius = config.getInt("guards.default.radius", 8);
        int radius = radiusOverride != null ? radiusOverride : defaultRadius;

        NPC npc = registry.createNPC(EntityType.PLAYER, name);
        npc.addTrait(GuardTrait.class);
        npc.spawn(creator.getLocation());

        GuardTrait trait = npc.getTrait(GuardTrait.class);
        trait.setHome(creator.getLocation());
        trait.setRadius(radius);
        trait.setLabel(name);

        guards.put(npc.getUniqueId(), npc);
        return npc;
    }

    public void removeGuard(NPC npc) {
        guards.remove(npc.getUniqueId());
        if (npc.isSpawned()) {
            npc.despawn();
        }
        registry.deregister(npc);
    }

    public Collection<NPC> getGuards() {
        return guards.values();
    }

    public NPCRegistry getRegistry() {
        return registry;
    }

    public Collection<NPC> getAllGuardNPCs() {
        Collection<NPC> found = new ArrayList<>();
        for (NPC npc : registry) {
            if (npc.hasTrait(GuardTrait.class)) {
                found.add(npc);
            }
        }
        return found;
    }

    public void shutdown() {
        for (NPC npc : guards.values()) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
        }
        guards.clear();
    }
}
