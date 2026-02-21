package com.metatask.mtguards;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MTGuardsCommand implements CommandExecutor, TabCompleter {

    private final GuardManager guardManager;

    public MTGuardsCommand(GuardManager guardManager) {
        this.guardManager = guardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <create|remove|list|reload|setradius|strict|attack|skin|equipweapon|equiparmor|stationary|pattern|info|setlabel|setname>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can create guards.");
                    return true;
                }
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " create <name> [radius]");
                    return true;
                }
                String name = args[1];
                Integer radius = null;
                if (args.length >= 3) {
                    try {
                        radius = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Radius must be a number.");
                        return true;
                    }
                }
                Player player = (Player) sender;
                NPC npc = guardManager.createGuard(player, name, radius);
                sender.sendMessage(ChatColor.GREEN + "Created guard NPC with id " + npc.getId() + " at your location.");
                return true;
            }
            case "remove": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " remove <npcId>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "NPC id must be a number.");
                    return true;
                }
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                guardManager.removeGuard(npc);
                sender.sendMessage(ChatColor.GREEN + "Removed guard NPC " + id + ".");
                return true;
            }
            case "list": {
                sender.sendMessage(ChatColor.AQUA + "MTGuards NPCs:");
                List<NPC> guards = new ArrayList<>(guardManager.getAllGuardNPCs());
                if (guards.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No guards have been found.");
                    return true;
                }
                guards.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
                for (NPC npc : guards) {
                    String loc = npc.isSpawned()
                            ? npc.getEntity().getWorld().getName() + " "
                            + npc.getEntity().getLocation().getBlockX() + ","
                            + npc.getEntity().getLocation().getBlockY() + ","
                            + npc.getEntity().getLocation().getBlockZ()
                            : "not spawned";
                    String labelText = npc.getOrAddTrait(GuardTrait.class).getLabel();
                    sender.sendMessage(ChatColor.GRAY + "- #" + npc.getId() + " [" + labelText + "] " + npc.getName() + " @ " + loc);
                }
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                MTGuardsPlugin.getInstance().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "MTGuards configuration reloaded.");
                return true;
            }
            case "setradius": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setradius <npcId> <radius>");
                    return true;
                }
                int id;
                int radius;
                try {
                    id = Integer.parseInt(args[1]);
                    radius = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId and radius must be numbers.");
                    return true;
                }
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                trait.setRadius(radius);
                sender.sendMessage(ChatColor.GREEN + "Set guard #" + id + " radius to " + radius + ".");
                return true;
            }
            case "strict": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " strict <npcId> <true|false>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                boolean value = Boolean.parseBoolean(args[2]);
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                trait.setStrictRadius(value);
                sender.sendMessage(ChatColor.GREEN + "Set guard #" + id + " strict radius to " + value + ".");
                return true;
            }
            case "attack": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " attack <npcId> <passive|hostile|players> <true|false>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                String mode = args[2].toLowerCase(Locale.ROOT);
                boolean value = Boolean.parseBoolean(args[3]);
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                if (mode.equals("passive")) {
                    trait.setAttackPassive(value);
                } else if (mode.equals("hostile")) {
                    trait.setAttackHostile(value);
                } else if (mode.equals("players")) {
                    trait.setAttackPlayers(value);
                } else {
                    sender.sendMessage(ChatColor.RED + "Mode must be passive, hostile, or players.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Updated guard #" + id + " attack " + mode + " to " + value + ".");
                return true;
            }
            case "skin": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " skin <npcId> <playerName>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                String skinName = args[2];
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                skinTrait.setSkinName(skinName);
                sender.sendMessage(ChatColor.GREEN + "Set guard #" + id + " skin to " + skinName + ".");
                return true;
            }
            case "equipweapon": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can equip guards.");
                    return true;
                }
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " equipweapon <npcId>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                Player player = (Player) sender;
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "You must hold a weapon in your main hand.");
                    return true;
                }
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                if (!npc.isSpawned() || !(npc.getEntity() instanceof LivingEntity)) {
                    sender.sendMessage(ChatColor.RED + "Guard must be spawned to equip items.");
                    return true;
                }
                LivingEntity entity = (LivingEntity) npc.getEntity();
                EntityEquipment eq = entity.getEquipment();
                if (eq != null) {
                    eq.setItemInMainHand(inHand.clone());
                }
                sender.sendMessage(ChatColor.GREEN + "Equipped guard #" + id + " with your held item.");
                return true;
            }
            case "equiparmor": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can equip guards.");
                    return true;
                }
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " equiparmor <npcId>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                Player player = (Player) sender;
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                if (!npc.isSpawned() || !(npc.getEntity() instanceof LivingEntity)) {
                    sender.sendMessage(ChatColor.RED + "Guard must be spawned to equip items.");
                    return true;
                }
                LivingEntity entity = (LivingEntity) npc.getEntity();
                EntityEquipment eq = entity.getEquipment();
                if (eq != null) {
                    eq.setHelmet(cloneOrNull(player.getInventory().getHelmet()));
                    eq.setChestplate(cloneOrNull(player.getInventory().getChestplate()));
                    eq.setLeggings(cloneOrNull(player.getInventory().getLeggings()));
                    eq.setBoots(cloneOrNull(player.getInventory().getBoots()));
                }
                sender.sendMessage(ChatColor.GREEN + "Equipped guard #" + id + " with your armor.");
                return true;
            }
            case "stationary": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " stationary <npcId> <true|false>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                boolean value = Boolean.parseBoolean(args[2]);
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                trait.setStationary(value);
                sender.sendMessage(ChatColor.GREEN + "Set guard #" + id + " stationary to " + value + ".");
                return true;
            }
            case "pattern": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " pattern <npcId> <fromselection|clear>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "npcId must be a number.");
                    return true;
                }
                String mode = args[2].toLowerCase(Locale.ROOT);
                NPC npc = guardManager.getRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                if (mode.equals("clear")) {
                    trait.clearPattern();
                    sender.sendMessage(ChatColor.GREEN + "Cleared patrol pattern for guard #" + id + ".");
                    return true;
                }
                if (!mode.equals("fromselection")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " pattern <npcId> <fromselection|clear>");
                    return true;
                }
                Player player = (Player) sender;
                WorldEditPlugin we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
                if (we == null) {
                    sender.sendMessage(ChatColor.RED + "WorldEdit is not installed.");
                    return true;
                }
                Region region;
                try {
                    region = we.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "You must have a WorldEdit selection.");
                    return true;
                }
                List<Location> points = new ArrayList<>();
                int limit = 200;
                for (BlockVector3 bv : region) {
                    if (points.size() >= limit) {
                        break;
                    }
                    int x = bv.getBlockX();
                    int z = bv.getBlockZ();
                    int groundY = player.getWorld().getHighestBlockYAt(x, z) + 1;
                    Location loc = new Location(player.getWorld(), x + 0.5, groundY, z + 0.5);
                    points.add(loc);
                }
                if (points.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Selection is empty.");
                    return true;
                }
                points.sort(Comparator
                        .comparingInt((Location l) -> l.getBlockX())
                        .thenComparingInt(Location::getBlockZ)
                        .thenComparingDouble(Location::getY));
                trait.setPattern(points);
                trait.setStationary(false);
                sender.sendMessage(ChatColor.GREEN + "Set patrol pattern for guard #" + id + " from your WorldEdit selection.");
                return true;
            }
            case "info": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " info <npcId>");
                    return true;
                }
                NPC npc = resolveNpc(args[1]);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found by id/name/label.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                String skinName = skinTrait.getSkinName();
                if (skinName == null || skinName.isEmpty()) {
                    skinName = npc.getName();
                }
                sender.sendMessage(ChatColor.AQUA + "Guard #" + npc.getId() + " info:");
                sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.WHITE + npc.getName());
                sender.sendMessage(ChatColor.GRAY + "Label: " + ChatColor.WHITE + trait.getLabel());
                sender.sendMessage(ChatColor.GRAY + "Skin: " + ChatColor.WHITE + skinName);
                sender.sendMessage(ChatColor.GRAY + "Radius: " + ChatColor.WHITE + trait.getRadius()
                        + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Strict: " + ChatColor.WHITE + trait.isStrictRadius());
                sender.sendMessage(ChatColor.GRAY + "Attack passive: " + ChatColor.WHITE + trait.isAttackPassive()
                        + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "hostile: " + ChatColor.WHITE + trait.isAttackHostile()
                        + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "players: " + ChatColor.WHITE + trait.isAttackPlayers());
                String perm = trait.getAttackPlayersPermission();
                sender.sendMessage(ChatColor.GRAY + "Players permission: " + ChatColor.WHITE + (perm == null ? "" : perm));
                return true;
            }
            case "setlabel": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setlabel <npcId|name|label> <newLabel>");
                    return true;
                }
                NPC npc = resolveNpc(args[1]);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found by id/name/label.");
                    return true;
                }
                GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
                String newLabel = ChatColor.translateAlternateColorCodes('&', args[2]);
                trait.setLabel(newLabel);
                sender.sendMessage(ChatColor.GREEN + "Updated guard #" + npc.getId() + " label to " + newLabel + ".");
                return true;
            }
            case "setname": {
                if (!sender.hasPermission("mtguards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setname <npcId|name|label> <newName>");
                    return true;
                }
                NPC npc = resolveNpc(args[1]);
                if (npc == null) {
                    sender.sendMessage(ChatColor.RED + "NPC not found by id/name/label.");
                    return true;
                }
                String newName = ChatColor.translateAlternateColorCodes('&', args[2]);
                npc.setName(newName);
                if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) npc.getEntity();
                    entity.setCustomName(newName);
                    entity.setCustomNameVisible(true);
                }
                sender.sendMessage(ChatColor.GREEN + "Updated guard #" + npc.getId() + " name to " + newName + ".");
                return true;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
        }
    }

    private ItemStack cloneOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.clone();
    }

    private NPC resolveNpc(String token) {
        String plainToken = ChatColor.stripColor(token);
        try {
            int id = Integer.parseInt(plainToken);
            NPC byId = guardManager.getRegistry().getById(id);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {}
        for (NPC npc : guardManager.getAllGuardNPCs()) {
            GuardTrait trait = npc.getOrAddTrait(GuardTrait.class);
            String npcName = ChatColor.stripColor(npc.getName());
            String labelText = trait.getLabel();
            String plainLabel = labelText != null ? ChatColor.stripColor(labelText) : null;
            if (npcName.equalsIgnoreCase(plainToken)
                    || npc.getName().equalsIgnoreCase(token)
                    || (plainLabel != null && plainLabel.equalsIgnoreCase(plainToken))
                    || (labelText != null && labelText.equalsIgnoreCase(token))) {
                return npc;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("create");
            subs.add("remove");
            subs.add("list");
            subs.add("setradius");
            subs.add("strict");
            subs.add("attack");
            subs.add("skin");
            subs.add("equipweapon");
            subs.add("equiparmor");
            subs.add("stationary");
            subs.add("pattern");
            subs.add("info");
            subs.add("setlabel");
            subs.add("setname");
            if (sender.hasPermission("mtguards.admin")) {
                subs.add("reload");
            }
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("setradius")
                || args[0].equalsIgnoreCase("strict")
                || args[0].equalsIgnoreCase("attack")
                || args[0].equalsIgnoreCase("skin")
                || args[0].equalsIgnoreCase("equipweapon")
                || args[0].equalsIgnoreCase("equiparmor")
                || args[0].equalsIgnoreCase("stationary")
                || args[0].equalsIgnoreCase("pattern")
                || args[0].equalsIgnoreCase("info")
                || args[0].equalsIgnoreCase("setlabel")
                || args[0].equalsIgnoreCase("setname"))) {
            List<String> tokens = new ArrayList<>();
            for (NPC npc : guardManager.getAllGuardNPCs()) {
                tokens.add(String.valueOf(npc.getId()));
                tokens.add(npc.getName());
                String labelText = npc.getOrAddTrait(GuardTrait.class).getLabel();
                if (labelText != null && !labelText.isEmpty()) {
                    tokens.add(labelText);
                }
            }
            return tokens.stream()
                    .filter(t -> t.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("attack")) {
            List<String> modes = new ArrayList<>();
            modes.add("passive");
            modes.add("hostile");
            modes.add("players");
            return modes.stream()
                    .filter(m -> m.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pattern")) {
            List<String> modes = new ArrayList<>();
            modes.add("fromselection");
            modes.add("clear");
            return modes.stream()
                    .filter(m -> m.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
