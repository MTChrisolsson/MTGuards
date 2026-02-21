package com.metatask.mtguards;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MTGuardsPlugin extends JavaPlugin {

    private static MTGuardsPlugin instance;
    private GuardManager guardManager;

    @Override
    public void onEnable() {
        instance = this;

        Plugin citizens = getServer().getPluginManager().getPlugin("Citizens");
        if (citizens == null || !citizens.isEnabled()) {
            getLogger().severe("Citizens plugin not found or not enabled. Disabling MTGuards.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        this.guardManager = new GuardManager(this, registry);

        new ReadmeYmlWriter(this).write();

        TraitInfo traitInfo = TraitInfo.create(GuardTrait.class).withName("mtguard");
        CitizensAPI.getTraitFactory().registerTrait(traitInfo);

        if (getCommand("mtguards") != null) {
            MTGuardsCommand cmd = new MTGuardsCommand(this.guardManager);
            getCommand("mtguards").setExecutor(cmd);
            getCommand("mtguards").setTabCompleter(cmd);
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[MTGuards] Enabled with Citizens integration.");
    }

    @Override
    public void onDisable() {
        if (guardManager != null) {
            guardManager.shutdown();
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[MTGuards] Disabled.");
    }

    public static MTGuardsPlugin getInstance() {
        return instance;
    }

    public GuardManager getGuardManager() {
        return guardManager;
    }
}
