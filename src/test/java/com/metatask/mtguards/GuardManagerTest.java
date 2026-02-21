package com.metatask.mtguards;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GuardManagerTest {

    private FileConfiguration config;
    private NPCRegistry registry;
    private GuardManager guardManager;

    @BeforeEach
    void setUp() {
        config = new YamlConfiguration();
        registry = mock(NPCRegistry.class);
        guardManager = new GuardManager(config, registry);
    }

    @Test
    void createGuard_usesDefaultRadiusWhenOverrideNull() {
        config.set("guards.default.radius", 8);

        Player creator = mock(Player.class);
        when(creator.getLocation()).thenReturn(new Location(null, 0, 64, 0));

        NPC npc = mock(NPC.class);
        GuardTrait trait = mock(GuardTrait.class);
        when(registry.createNPC(EntityType.PLAYER, "TestGuard")).thenReturn(npc);
        when(npc.getTrait(GuardTrait.class)).thenReturn(trait);
        UUID npcId = UUID.randomUUID();
        when(npc.getUniqueId()).thenReturn(npcId);

        guardManager.createGuard(creator, "TestGuard", null);

        verify(trait).setRadius(8);
        verify(trait).setHome(creator.getLocation());
        verify(trait).setLabel("TestGuard");
        verify(npc).spawn(creator.getLocation());
    }

    @Test
    void createGuard_usesOverrideRadiusWhenProvided() {
        config.set("guards.default.radius", 8);

        Player creator = mock(Player.class);
        when(creator.getLocation()).thenReturn(new Location(null, 10, 70, -5));

        NPC npc = mock(NPC.class);
        GuardTrait trait = mock(GuardTrait.class);
        when(registry.createNPC(EntityType.PLAYER, "OverrideGuard")).thenReturn(npc);
        when(npc.getTrait(GuardTrait.class)).thenReturn(trait);
        UUID npcId = UUID.randomUUID();
        when(npc.getUniqueId()).thenReturn(npcId);

        guardManager.createGuard(creator, "OverrideGuard", 16);

        verify(trait).setRadius(16);
    }

    @Test
    void getAllGuardNPCs_returnsOnlyNPCsWithGuardTrait() {
        NPC withTrait = mock(NPC.class);
        when(withTrait.hasTrait(GuardTrait.class)).thenReturn(true);
        NPC withoutTrait = mock(NPC.class);
        when(withoutTrait.hasTrait(GuardTrait.class)).thenReturn(false);

        Iterator<NPC> iterator = Arrays.asList(withTrait, withoutTrait).iterator();
        when(registry.iterator()).thenReturn(iterator);

        Collection<NPC> result = guardManager.getAllGuardNPCs();

        assertEquals(1, result.size());
        assertEquals(withTrait, result.iterator().next());
        verify(withTrait).hasTrait(eq(GuardTrait.class));
        verify(withoutTrait).hasTrait(eq(GuardTrait.class));
    }
}
