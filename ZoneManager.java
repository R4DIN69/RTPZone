package com.rtpzone.zone;

import com.rtpzone.RTPZonePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the RTP hole zone creation, boundaries, and visual particle vortex.
 */
public class ZoneManager {

    private final RTPZonePlugin plugin;
    private boolean configured;
    private Location center;
    private double radius;
    private double depthRange;
    private BukkitTask particleTask;

    public ZoneManager(RTPZonePlugin plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    /**
     * Loads the zone coordinates and parameters from config.yml.
     */
    public void loadFromConfig() {
        cancelParticleTask();
        FileConfiguration config = plugin.getConfig();
        this.configured = config.getBoolean("zone.configured", false);
        if (configured) {
            String worldName = config.getString("zone.world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("zone.x", 0.0);
                double y = config.getDouble("zone.y", 64.0);
                double z = config.getDouble("zone.z", 0.0);
                this.center = new Location(world, x, y, z);
                this.radius = config.getDouble("zone.radius", 1.8);
                this.depthRange = config.getDouble("zone.depth-range", 8.0);
                startParticleTask();
            } else {
                plugin.getLogger().warning("RTP zone world '" + worldName + "' not found!");
                this.configured = false;
            }
        }
    }

    /**
     * Selects and creates a hole-type RTP area right in front of the player.
     *
     * @param player The admin player executing the command
     * @return The location where the zone was created
     */
    public Location createHoleZone(Player player) {
        // Calculate location 3 blocks directly in front of the player
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0));
        World world = targetLoc.getWorld();

        // Round to clean block coordinates
        int targetX = targetLoc.getBlockX();
        int targetY = targetLoc.getBlockY();
        int targetZ = targetLoc.getBlockZ();

        FileConfiguration config = plugin.getConfig();

        // Physical hole digging if enabled
        boolean digHole = config.getBoolean("zone.physical-hole.enabled", true);
        if (digHole && world != null) {
            int holeRadius = Math.max(1, config.getInt("zone.physical-hole.radius", 1));
            int holeDepth = Math.max(2, config.getInt("zone.physical-hole.depth", 6));
            String hollowMatName = config.getString("zone.physical-hole.hollow-block", "AIR");
            Material hollowMat = Material.matchMaterial(hollowMatName);
            if (hollowMat == null) hollowMat = Material.AIR;

            boolean borderEnabled = config.getBoolean("zone.physical-hole.border-enabled", true);
            String borderMatName = config.getString("zone.physical-hole.border-block", "CRYING_OBSIDIAN");
            Material borderMat = Material.matchMaterial(borderMatName);
            if (borderMat == null) borderMat = Material.CRYING_OBSIDIAN;

            // Excavate the hole
            for (int dx = -holeRadius; dx <= holeRadius; dx++) {
                for (int dz = -holeRadius; dz <= holeRadius; dz++) {
                    for (int dy = 0; dy <= holeDepth; dy++) {
                        Block b = world.getBlockAt(targetX + dx, targetY - dy, targetZ + dz);
                        b.setType(hollowMat, false);
                    }
                }
            }

            // Create decorative border ring around the top if enabled
            if (borderEnabled) {
                for (int dx = -holeRadius - 1; dx <= holeRadius + 1; dx++) {
                    for (int dz = -holeRadius - 1; dz <= holeRadius + 1; dz++) {
                        // Only perimeter
                        if (Math.abs(dx) == holeRadius + 1 || Math.abs(dz) == holeRadius + 1) {
                            Block borderBlock = world.getBlockAt(targetX + dx, targetY, targetZ + dz);
                            borderBlock.setType(borderMat, false);
                        }
                    }
                }
            }
        }

        // Center location for detection and particles
        this.center = new Location(world, targetX + 0.5, targetY + 0.5, targetZ + 0.5);
        this.radius = config.getDouble("zone.radius", 1.8);
        this.depthRange = config.getDouble("zone.depth-range", 8.0);
        this.configured = true;

        // Save to config.yml
        config.set("zone.configured", true);
        config.set("zone.world", world.getName());
        config.set("zone.x", this.center.getX());
        config.set("zone.y", this.center.getY());
        config.set("zone.z", this.center.getZ());
        plugin.saveConfig();

        // Restart visual particle vortex
        startParticleTask();

        // Sound effect on zone creation
        player.playSound(this.center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);

        return this.center;
    }

    /**
     * Removes the configured RTP zone.
     */
    public void removeZone() {
        this.configured = false;
        cancelParticleTask();
        FileConfiguration config = plugin.getConfig();
        config.set("zone.configured", false);
        plugin.saveConfig();
    }

    /**
     * Checks if a given location is inside the hole zone cylinder.
     */
    public boolean isInsideZone(Location loc) {
        if (!configured || center == null || loc == null) return false;
        if (!loc.getWorld().equals(center.getWorld())) return false;

        double horizontalDistanceSq = Math.pow(loc.getX() - center.getX(), 2) + Math.pow(loc.getZ() - center.getZ(), 2);
        if (horizontalDistanceSq > (radius * radius)) {
            return false;
        }

        // Must be at the surface level or falling downwards inside the hole
        double yDiff = center.getY() - loc.getY();
        return yDiff >= -1.0 && yDiff <= depthRange;
    }

    /**
     * Starts continuous swirling particle vortex animation in the hole.
     */
    private void startParticleTask() {
        cancelParticleTask();
        if (!plugin.getConfig().getBoolean("zone.particles.enabled", true)) {
            return;
        }

        String partName = plugin.getConfig().getString("zone.particles.particle", "PORTAL");
        Particle primaryParticle;
        try {
            primaryParticle = Particle.valueOf(partName.toUpperCase());
        } catch (IllegalArgumentException e) {
            primaryParticle = Particle.PORTAL;
        }

        String secPartName = plugin.getConfig().getString("zone.particles.secondary-particle", "REVERSE_PORTAL");
        Particle secondaryParticle;
        try {
            secondaryParticle = Particle.valueOf(secPartName.toUpperCase());
        } catch (IllegalArgumentException e) {
            secondaryParticle = Particle.REVERSE_PORTAL;
        }

        final Particle particle1 = primaryParticle;
        final Particle particle2 = secondaryParticle;
        final int points = plugin.getConfig().getInt("zone.particles.points-per-ring", 20);

        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private double angle = 0;

            @Override
            public void run() {
                if (!configured || center == null || center.getWorld() == null) {
                    return;
                }

                World w = center.getWorld();
                angle += Math.PI / 10;
                if (angle >= Math.PI * 2) {
                    angle = 0;
                }

                // Draw swirling vortex rings going down the hole
                for (int ring = 0; ring < 4; ring++) {
                    double yOffset = -ring * 1.2;
                    double ringRadius = Math.max(0.4, radius - (ring * 0.25));

                    for (int i = 0; i < points; i++) {
                        double currentAngle = angle + (i * (2 * Math.PI / points));
                        double x = center.getX() + (ringRadius * Math.cos(currentAngle));
                        double z = center.getZ() + (ringRadius * Math.sin(currentAngle));

                        w.spawnParticle(particle1, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0.02);
                    }
                }

                // Secondary particle in the center core
                w.spawnParticle(particle2, center.getX(), center.getY() - 1.5, center.getZ(), 4, 0.3, 0.8, 0.3, 0.05);
            }
        }, 0L, 3L);
    }

    public void cancelParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public Location getCenter() {
        return center;
    }
}
