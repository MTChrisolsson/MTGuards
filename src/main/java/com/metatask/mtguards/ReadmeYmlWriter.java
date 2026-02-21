package com.metatask.mtguards;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReadmeYmlWriter {

    private final MTGuardsPlugin plugin;

    public ReadmeYmlWriter(MTGuardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void write() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File readme = new File(dataFolder, "readme.yml");
            YamlConfiguration yaml = new YamlConfiguration();

            yaml.set("plugin.name", plugin.getDescription().getName());
            yaml.set("plugin.version", plugin.getDescription().getVersion());
            yaml.set("plugin.generatedAt", System.currentTimeMillis());

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("guards.default.radius", "Integer - default roam radius in blocks");
            config.put("guards.default.strictRadius", "Boolean - clamp targets to radius when true");
            config.put("guards.default.attackPassive", "Boolean - attack passive mobs");
            config.put("guards.default.attackHostile", "Boolean - attack hostile mobs (Monster)");
            config.put("guards.default.attackPlayers", "Boolean - attack all players");
            config.put("guards.default.attackPlayersWithPermission", "String - permission node; attack only players with this node if attackPlayers=false");
            config.put("guards.default.neverAttackOps", "Boolean - never attack server operators");
            config.put("guards.default.safePlayerPermission", "String - players with this permission are never attacked");
            config.put("behavior.tickInterval", "Integer - AI tick interval in server ticks");
            config.put("behavior.targetScanRadius", "Integer - target scan radius");
            config.put("behavior.walkSpeed", "Double - Citizens navigator speed");
            config.put("behavior.attackCooldownMs", "Integer - minimum milliseconds between attacks");
            yaml.set("config_keys", config);

            Map<String, Object> permissions = new LinkedHashMap<>();
            permissions.put("mtguards.use", "Access to /mtguards basic use");
            permissions.put("mtguards.admin", "Administrative commands");
            permissions.put("mtguards.admin.create", "Create guards");
            permissions.put("mtguards.admin.remove", "Remove guards");
            permissions.put("mtguards.admin.list", "List guards");
            permissions.put("mtguards.admin.reload", "Reload config");
            permissions.put("mtguards.admin.setradius", "Set guard radius");
            permissions.put("mtguards.admin.strict", "Toggle strict radius");
            permissions.put("mtguards.admin.attack", "Configure attack behaviour");
            permissions.put("mtguards.admin.skin", "Change guard skin");
            permissions.put("mtguards.admin.equipweapon", "Equip guard weapon");
            permissions.put("mtguards.admin.equiparmor", "Equip guard armor");
            permissions.put("mtguards.admin.stationary", "Toggle stationary mode");
            permissions.put("mtguards.admin.pattern", "Set patrol pattern from WorldEdit selection");
            permissions.put("mtguards.admin.info", "View guard info");
            permissions.put("mtguards.admin.setlabel", "Set guard label (alias)");
            permissions.put("mtguards.admin.setname", "Change guard display name");
            permissions.put("mtguards.safe", "Players with this are never attacked");
            permissions.put("mtguards.hostile", "Players with this may be attacked when attackPlayers=false");
            permissions.put("mtguards.admin.teleport", "Teleport a guard to a location or player");
            yaml.set("permissions", permissions);

            Map<String, Object> commands = new LinkedHashMap<>();
            commands.put("/mtguards create <name> [radius]", "Create guard at player location");
            commands.put("/mtguards remove <id|name|label>", "Remove guard");
            commands.put("/mtguards list", "List all guards");
            commands.put("/mtguards reload", "Reload plugin config");
            commands.put("/mtguards setradius <id|name|label> <radius>", "Set guard radius");
            commands.put("/mtguards strict <id|name|label> <true|false>", "Toggle strict radius");
            commands.put("/mtguards attack <id|name|label> <passive|hostile|players> <true|false>", "Configure attack behaviour");
            commands.put("/mtguards skin <id|name|label> <playerName>", "Set skin from username");
            commands.put("/mtguards equipweapon <id|name|label>", "Equip from held item");
            commands.put("/mtguards equiparmor <id|name|label>", "Equip from worn armor");
            commands.put("/mtguards stationary <id|name|label> <true|false>", "Toggle stationary mode");
            commands.put("/mtguards pattern <id|name|label> <fromselection|clear>", "Set patrol pattern from WorldEdit selection or clear");
            commands.put("/mtguards info <id|name|label>", "Show guard info");
            commands.put("/mtguards setlabel <id|name|label> <newLabel>", "Set human-friendly label");
            commands.put("/mtguards setname <id|name|label> <newName>", "Set guard display name with color codes");
            commands.put("/mtguards teleport <id|name|label> here", "Teleport guard to your current location (included pitch & yaw)");
            commands.put("/mtguards teleport <id|name|label> <world> <x> <y> <z> [yaw] [pitch]", "Teleport guard to a location (optional yaw & pitch)");
            yaml.set("commands", commands);

            yaml.save(readme);
        } catch (IOException ignored) {
        }
    }
}

