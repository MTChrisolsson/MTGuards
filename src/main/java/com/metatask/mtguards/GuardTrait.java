package com.metatask.mtguards;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Base64;

public class GuardTrait extends Trait implements Runnable {

    private Location home;
    private int radius;
    private boolean strictRadius;
    private boolean attackPassive;
    private boolean attackHostile;
    private boolean attackPlayers;
    private String attackPlayersPermission;
    private boolean neverAttackOps;
    private String safePlayerPermission;
    private BukkitTask task;
    private UUID currentTargetId;
    private long lastAttackTime;
    private boolean stationary;
    private final List<Location> pattern = new ArrayList<>();
    private int patternIndex;
    private int scanRadius = 10;
    private float walkSpeed = 1.2F;
    private int attackCooldownMs = 500;
    private String label;
    private ItemStack savedHelmet;
    private ItemStack savedChest;
    private ItemStack savedLegs;
    private ItemStack savedBoots;
    private ItemStack savedMainHand;

    public GuardTrait() {
        super("mtguard");
        this.radius = 8;
        boolean strict = false;
        boolean ap = true;
        boolean ah = true;
        boolean atp = false;
        String perm = null;
        MTGuardsPlugin plugin = MTGuardsPlugin.getInstance();
        if (plugin != null) {
            strict = plugin.getConfig().getBoolean("guards.default.strictRadius", false);
            ap = plugin.getConfig().getBoolean("guards.default.attackPassive", true);
            ah = plugin.getConfig().getBoolean("guards.default.attackHostile", true);
            atp = plugin.getConfig().getBoolean("guards.default.attackPlayers", false);
            String p = plugin.getConfig().getString("guards.default.attackPlayersWithPermission");
            if (p != null && !p.isEmpty()) {
                perm = p;
            }
            this.neverAttackOps = plugin.getConfig().getBoolean("guards.default.neverAttackOps", true);
            String safePerm = plugin.getConfig().getString("guards.default.safePlayerPermission", "mtguards.safe");
            this.safePlayerPermission = (safePerm != null && !safePerm.isEmpty()) ? safePerm : null;
            this.scanRadius = plugin.getConfig().getInt("behavior.targetScanRadius", this.scanRadius);
            this.walkSpeed = (float) plugin.getConfig().getDouble("behavior.walkSpeed", this.walkSpeed);
            this.attackCooldownMs = plugin.getConfig().getInt("behavior.attackCooldownMs", this.attackCooldownMs);
        }
        this.strictRadius = strict;
        this.attackPassive = ap;
        this.attackHostile = ah;
        this.attackPlayers = atp;
        this.attackPlayersPermission = perm;
        this.label = null;
    }

    @Override
    public void run() {
        if (npc == null || !npc.isSpawned()) {
            return;
        }
        LivingEntity entity = (LivingEntity) npc.getEntity();
        if (entity == null || entity.isDead()) {
            return;
        }
        Location base = home != null ? home : entity.getLocation();
        double distSq = entity.getLocation().distanceSquared(base);
        if (!stationary && distSq > radius * radius) {
            npc.getNavigator().setTarget(base);
            return;
        }
        if (stationary && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        scanAndAttack(entity, base);
        if (!stationary) {
            if (!pattern.isEmpty()) {
                patrol(entity);
            } else {
                roam(entity, base);
            }
        }
        updateLookDirection(entity, base);
    }

    private void scanAndAttack(LivingEntity self, Location base) {
        List<Entity> nearby = self.getNearbyEntities(scanRadius, scanRadius, scanRadius);
        LivingEntity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity)) {
                continue;
            }
            if (e == self) {
                continue;
            }
            LivingEntity candidate = (LivingEntity) e;
            if (!isTargetAllowed(candidate, base)) {
                continue;
            }
            if (!e.getWorld().equals(base.getWorld())) {
                continue;
            }
            double dist = self.getLocation().distanceSquared(candidate.getLocation());
            if (dist < bestDistSq) {
                bestDistSq = dist;
                bestTarget = candidate;
            }
        }
        if (bestTarget == null) {
            currentTargetId = null;
            return;
        }
        if (!stationary) {
            boolean needNavigate = !npc.getNavigator().isNavigating() || !bestTarget.getUniqueId().equals(currentTargetId);
            if (needNavigate) {
                navigateToTarget(self, bestTarget);
            }
        }
        currentTargetId = bestTarget.getUniqueId();
        if (bestDistSq <= 4.0) {
            long now = System.currentTimeMillis();
            if (now - lastAttackTime >= attackCooldownMs) {
                if (self instanceof HumanEntity) {
                    ((HumanEntity) self).swingMainHand();
                }
                self.attack(bestTarget);
                lastAttackTime = now;
            }
        }
    }

    private void navigateToTarget(LivingEntity self, LivingEntity target) {
        Location targetLoc = target.getLocation();
        Location from = self.getLocation();
        Location navTarget = targetLoc;
        double dx = targetLoc.getX() - from.getX();
        double dz = targetLoc.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double dy = from.getY() - targetLoc.getY();
        if (dy >= 0.9 && horizontalDist <= 2.5) {
            Location ground = targetLoc.clone();
            ground.setY(self.getWorld().getHighestBlockYAt(ground) + 1.0);
            navTarget = ground;
        }
        npc.getNavigator().getLocalParameters().speed(walkSpeed);
        npc.getNavigator().setTarget(navTarget);
    }

    private boolean isTargetAllowed(LivingEntity target, Location base) {
        if (strictRadius && home != null) {
            double dist = target.getLocation().distanceSquared(home);
            if (dist > radius * radius) {
                return false;
            }
        }
        if (target instanceof Player) {
            Player p = (Player) target;
            if (neverAttackOps && p.isOp()) {
                return false;
            }
            if (safePlayerPermission != null && !safePlayerPermission.isEmpty() && p.hasPermission(safePlayerPermission)) {
                return false;
            }
            if (attackPlayers) {
                return true;
            }
            if (attackPlayersPermission != null && !attackPlayersPermission.isEmpty()) {
                if (p.hasPermission(attackPlayersPermission)) {
                    return true;
                }
            }
            return false;
        }
        if (target instanceof Monster) {
            return attackHostile;
        }
        return attackPassive;
    }

    private void roam(LivingEntity self, Location base) {
        if (radius <= 0) {
            return;
        }
        if (npc.getNavigator().isNavigating()) {
            return;
        }
        Location current = self.getLocation();
        double distSq = current.distanceSquared(base);
        if (distSq > radius * radius) {
            npc.getNavigator().setTarget(base);
            return;
        }
        if (Math.random() < 0.05) {
            double dx = (Math.random() * radius * 2) - radius;
            double dz = (Math.random() * radius * 2) - radius;
            Location target = base.clone().add(dx, 0, dz);
            target.setY(self.getWorld().getHighestBlockYAt(target) + 1.0);
            npc.getNavigator().setTarget(target);
        }
    }

    private void updateLookDirection(LivingEntity self, Location base) {
        if (npc.getNavigator().isNavigating()) {
            return;
        }
        LivingEntity target = getCurrentTarget();
        if (target != null && !target.isDead() && target.getWorld().equals(self.getWorld())) {
            lookAt(self, target.getLocation());
        } else {
            currentTargetId = null;
            lookForward(self);
        }
    }

    private LivingEntity getCurrentTarget() {
        if (currentTargetId == null) {
            return null;
        }
        Entity e = Bukkit.getEntity(currentTargetId);
        if (e instanceof LivingEntity) {
            return (LivingEntity) e;
        }
        return null;
    }

    private void lookAt(LivingEntity self, Location target) {
        Location from = self.getLocation();
        Location look = from.clone();
        look.setDirection(target.toVector().subtract(from.toVector()));
        self.teleport(look);
    }

    private void lookForward(LivingEntity self) {
        Location loc = self.getLocation();
        Location look = loc.clone();
        look.setPitch(0f);
        self.teleport(look);
    }

    @Override
    public void onAttach() {
        if (npc.hasTrait(Owner.class)) {
            Owner owner = npc.getTrait(Owner.class);
            UUID ownerId = owner.getOwnerId();
            if (ownerId != null) {
                Player p = Bukkit.getPlayer(ownerId);
                if (p != null) {
                    home = p.getLocation();
                }
            }
        }
        if (label == null) {
            label = npc.getName();
        }
    }

    @Override
    public void onSpawn() {
        int interval = MTGuardsPlugin.getInstance().getConfig().getInt("behavior.tickInterval", 20);
        task = Bukkit.getScheduler().runTaskTimer(MTGuardsPlugin.getInstance(), this, interval, interval);
        if (home == null && npc.isSpawned()) {
            home = npc.getEntity().getLocation();
        }
        npc.getNavigator().getDefaultParameters().speed(walkSpeed);
        if (label == null) {
            label = npc.getName();
        }
        if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) npc.getEntity();
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                if (savedHelmet != null) eq.setHelmet(savedHelmet.clone());
                if (savedChest != null) eq.setChestplate(savedChest.clone());
                if (savedLegs != null) eq.setLeggings(savedLegs.clone());
                if (savedBoots != null) eq.setBoots(savedBoots.clone());
                if (savedMainHand != null) eq.setItemInMainHand(savedMainHand.clone());
            }
        }
    }

    @Override
    public void onDespawn() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void onRemove() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isStrictRadius() {
        return strictRadius;
    }

    public void setStrictRadius(boolean strictRadius) {
        this.strictRadius = strictRadius;
    }

    public void setAttackPassive(boolean attackPassive) {
        this.attackPassive = attackPassive;
    }

    public boolean isAttackPassive() {
        return attackPassive;
    }

    public void setAttackHostile(boolean attackHostile) {
        this.attackHostile = attackHostile;
    }

    public boolean isAttackHostile() {
        return attackHostile;
    }

    public void setAttackPlayers(boolean attackPlayers) {
        this.attackPlayers = attackPlayers;
    }

    public boolean isAttackPlayers() {
        return attackPlayers;
    }

    public void setAttackPlayersPermission(String attackPlayersPermission) {
        this.attackPlayersPermission = attackPlayersPermission;
    }

    public String getAttackPlayersPermission() {
        return attackPlayersPermission;
    }

    public boolean isStationary() {
        return stationary;
    }

    public void setStationary(boolean stationary) {
        this.stationary = stationary;
    }

    public void setPattern(List<Location> locations) {
        pattern.clear();
        if (locations != null) {
            pattern.addAll(locations);
        }
        patternIndex = 0;
    }

    public void clearPattern() {
        pattern.clear();
        patternIndex = 0;
    }

    public boolean hasPattern() {
        return !pattern.isEmpty();
    }

    private void patrol(LivingEntity self) {
        if (pattern.isEmpty()) {
            return;
        }
        if (npc.getNavigator().isNavigating()) {
            return;
        }
        if (patternIndex < 0 || patternIndex >= pattern.size()) {
            patternIndex = 0;
        }
        Location target = pattern.get(patternIndex);
        if (self.getLocation().distanceSquared(target) <= 1.0) {
            patternIndex = (patternIndex + 1) % pattern.size();
            target = pattern.get(patternIndex);
        }
        Location ground = target.clone();
        ground.setY(self.getWorld().getHighestBlockYAt(ground) + 1.0);
        npc.getNavigator().getLocalParameters().speed(1.2F);
        npc.getNavigator().setTarget(ground);
    }

    @Override
    public void load(DataKey key) {
        this.radius = key.getInt("radius", this.radius);
        this.strictRadius = key.getBoolean("strictRadius", this.strictRadius);
        this.attackPassive = key.getBoolean("attackPassive", this.attackPassive);
        this.attackHostile = key.getBoolean("attackHostile", this.attackHostile);
        this.attackPlayers = key.getBoolean("attackPlayers", this.attackPlayers);
        this.stationary = key.getBoolean("stationary", this.stationary);
        this.neverAttackOps = key.getBoolean("neverAttackOps", this.neverAttackOps);
        String safePerm = key.getString("safePlayerPermission");
        if (safePerm != null && !safePerm.isEmpty()) {
            this.safePlayerPermission = safePerm;
        }
        String p = key.getString("attackPlayersPermission");
        if (p != null && !p.isEmpty()) {
            this.attackPlayersPermission = p;
        }
        String lbl = key.getString("label");
        if (lbl != null && !lbl.isEmpty()) {
            this.label = lbl;
        }
        String h = key.getString("equipment.helmet");
        String c = key.getString("equipment.chest");
        String l = key.getString("equipment.legs");
        String b = key.getString("equipment.boots");
        String m = key.getString("equipment.mainhand");
        if (h != null && !h.isEmpty()) savedHelmet = deserializeItem(h);
        if (c != null && !c.isEmpty()) savedChest = deserializeItem(c);
        if (l != null && !l.isEmpty()) savedLegs = deserializeItem(l);
        if (b != null && !b.isEmpty()) savedBoots = deserializeItem(b);
        if (m != null && !m.isEmpty()) savedMainHand = deserializeItem(m);
    }

    @Override
    public void save(DataKey key) {
        key.setInt("radius", radius);
        key.setBoolean("strictRadius", strictRadius);
        key.setBoolean("attackPassive", attackPassive);
        key.setBoolean("attackHostile", attackHostile);
        key.setBoolean("attackPlayers", attackPlayers);
        key.setBoolean("stationary", stationary);
        key.setBoolean("neverAttackOps", neverAttackOps);
        key.setString("safePlayerPermission", safePlayerPermission);
        key.setString("attackPlayersPermission", attackPlayersPermission);
        key.setString("label", label);
        key.setString("equipment.helmet", serializeItem(savedHelmet));
        key.setString("equipment.chest", serializeItem(savedChest));
        key.setString("equipment.legs", serializeItem(savedLegs));
        key.setString("equipment.boots", serializeItem(savedBoots));
        key.setString("equipment.mainhand", serializeItem(savedMainHand));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSavedArmor(ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        this.savedHelmet = cloneOrNull(helmet);
        this.savedChest = cloneOrNull(chest);
        this.savedLegs = cloneOrNull(legs);
        this.savedBoots = cloneOrNull(boots);
        if (npc != null && npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) npc.getEntity();
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                eq.setHelmet(savedHelmet);
                eq.setChestplate(savedChest);
                eq.setLeggings(savedLegs);
                eq.setBoots(savedBoots);
            }
        }
    }

    public void setSavedMainHand(ItemStack item) {
        this.savedMainHand = cloneOrNull(item);
        if (npc != null && npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) npc.getEntity();
            EntityEquipment eq = le.getEquipment();
            if (eq != null) {
                eq.setItemInMainHand(savedMainHand);
            }
        }
    }

    private ItemStack cloneOrNull(ItemStack item) {
        if (item == null) return null;
        return item.clone();
    }

    private String serializeItem(ItemStack item) {
        try {
            if (item == null) return "";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(out);
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack deserializeItem(String data) {
        try {
            if (data == null || data.isEmpty()) return null;
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(in);
            Object obj = ois.readObject();
            if (obj instanceof ItemStack) {
                return (ItemStack) obj;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
