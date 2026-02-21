package com.metatask.mtguards;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigDefaultsTest {

    @Test
    void defaultConfig_hasExpectedGuardAndBehaviorSettings() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(in, "config.yml should be on the classpath");

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in));

        assertEquals(8, cfg.getInt("guards.default.radius"));
        assertEquals(false, cfg.getBoolean("guards.default.strictRadius"));
        assertEquals(true, cfg.getBoolean("guards.default.attackPassive"));
        assertEquals(true, cfg.getBoolean("guards.default.attackHostile"));
        assertEquals(false, cfg.getBoolean("guards.default.attackPlayers"));
        assertEquals("mtguards.hostile", cfg.getString("guards.default.attackPlayersWithPermission"));
        assertEquals(true, cfg.getBoolean("guards.default.neverAttackOps"));
        assertEquals("mtguards.safe", cfg.getString("guards.default.safePlayerPermission"));

        assertEquals(20, cfg.getInt("behavior.tickInterval"));
        assertEquals(10, cfg.getInt("behavior.targetScanRadius"));
        assertEquals(1.2, cfg.getDouble("behavior.walkSpeed"));
        assertEquals(500, cfg.getInt("behavior.attackCooldownMs"));
    }
}

