package com.rtpzone.teleport;

import com.rtpzone.RTPZonePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles safe random location searching in the overworld,
 * the initial slow-falling drop into the hole, and teleportation 50 blocks above ground.
 */
public class RTPManager {

    private final RTPZonePlugin plugin;
    private final Random random = new Random();
    private final Set<UUID> teleportingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<Material> blacklistedMaterials = new HashSet<>();

    public RTPManager(RTPZonePlugin plugin) {
        this.plugin = plugin;
        loadBlacklist();
    }

    public void loadBlacklist() {
        blacklistedMaterials.clear();
        List<String> list = plugin.getConfig().getStringList("teleport.blacklisted-blocks");
        for (String name : list) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                blacklistedMaterials.add(mat);
            }
        }
        // Fallback safety defaults
        blacklistedMaterials.add(Material.WATER);
        blacklistedMaterials.add(Material.LAVA);
        blacklistedMaterials.add(Material.FIRE);
        blacklistedMaterials.add(Material.SOUL_FIRE);
        blacklistedMaterials.add(Material.CACTUS);
        blacklistedMaterials.add(Material.SWEET_BERRY_BUSH);
        blacklistedMaterials.add(Material.POWDER_SNOW);
    }

    /**
     * Initiates the teleportation sequence when a player enters the RTP hole:
     * 1. Applies Slow Falling so the player floats down the hole.
     * 2. Plays sounds and sends the notification message.
     * 3. After the configured delay, finds safe coordinates and teleports 50 blocks above the overworld.
     */
    public void initiateHoleTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        if (teleportingPlayers.contains(uuid) || plugin.getCameraManager().isDescending(player)) {
            return;
        }

        teleportingPlayers.add(uuid);

        FileConfiguration config = plugin.getConfig();

        // Apply slow falling for the hole descent
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false, true));

        // Play enter sound
        String enterSound = config.getString("zone.sounds.enter", "BLOCK_PORTAL_TRAVEL");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(enterSound), 1.0f, 1.0f);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 1.0f);
        }

        String prefix = config.getString("messages.prefix", "§8[§b§lRTP§3§lZone§8] ");
        String enterMsg = config.getString("messages.entering-hole", "§dYou stepped into the void hole... Falling into the unknown!");
        player.sendMessage(prefix + enterMsg);

        int delayTicks = config.getInt("teleport.hole-fall-delay-ticks", 35);

        // Schedule the random teleport
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                teleportingPlayers.remove(uuid);
                return;
            }

            findAndTeleport(player, 0);
        }, delayTicks);
    }

    /**
     * Recursively attempts to locate a safe surface in the overworld and teleports the player 50 blocks above it.
     */
    private void findAndTeleport(Player player, int attempt) {
        FileConfiguration config = plugin.getConfig();
        int maxAttempts = config.getInt("teleport.max-attempts", 30);

        if (attempt >= maxAttempts) {
            teleportingPlayers.remove(player.getUniqueId());
            String prefix = config.getString("messages.prefix", "§8[§b§lRTP§3§lZone§8] ");
            String failMsg = config.getString("messages.teleport-failed", "§cCould not find a safe location. Please try again.");
            player.sendMessage(prefix + failMsg);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            return;
        }

        // Target World: Overworld
        String targetWorldName = config.getString("teleport.target-world", "world");
        World world = Bukkit.getWorld(targetWorldName);
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            // Pick default overworld
            for (World w : Bukkit.getWorlds()) {
                if (w.getEnvironment() == World.Environment.NORMAL) {
                    world = w;
                    break;
                }
            }
        }

        if (world == null) {
            teleportingPlayers.remove(player.getUniqueId());
            player.sendMessage("§cError: Overworld world not found!");
            return;
        }

        int minRadius = config.getInt("teleport.min-radius", 500);
        int maxRadius = config.getInt("teleport.max-radius", 5000);

        // Pick random angle and distance
        int range = maxRadius - minRadius;
        int distance = minRadius + (range > 0 ? random.nextInt(range) : 0);
        double angle = random.nextDouble() * 2 * Math.PI;

        int targetX = (int) (distance * Math.cos(angle));
        int targetZ = (int) (distance * Math.sin(angle));

        final World finalWorld = world;
        final int nextAttempt = attempt + 1;

        // Use Paper's asynchronous chunk loading for high performance without server lag
        finalWorld.getChunkAtAsync(targetX >> 4, targetZ >> 4).thenAccept(chunk -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    teleportingPlayers.remove(player.getUniqueId());
                    return;
                }

                // Check highest solid block at (targetX, targetZ)
                int safeY = getSafeGroundY(finalWorld, targetX, targetZ);
                if (safeY == -1) {
                    // Unsafe location (ocean, lava, etc.) -> Try next random location
                    findAndTeleport(player, nextAttempt);
                    return;
                }

                // Exactly 50 blocks above ground as requested!
                int heightAboveGround = config.getInt("teleport.height-above-ground", 50);
                double spawnY = safeY + heightAboveGround;

                Location dest = new Location(finalWorld, targetX + 0.5, spawnY, targetZ + 0.5, player.getLocation().getYaw(), 0);

                // Teleport the player
                player.teleportAsync(dest).thenAccept(success -> {
                    if (success) {
                        teleportingPlayers.remove(player.getUniqueId());

                        // Slow falling effect for smooth descent from 50 blocks
                        int duration = config.getInt("effects.slow-falling-duration", 60) * 20;
                        int amplifier = config.getInt("effects.amplifier", 0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, amplifier, false, false, true));

                        // Activate 3rd person POV camera transition and descent effects
                        plugin.getCameraManager().activateThirdPersonDescent(player, safeY);

                        String prefix = config.getString("messages.prefix", "§8[§b§lRTP§3§lZone§8] ");
                        String tpMsg = config.getString("messages.teleporting", "§bTeleporting to a random overworld location...");
                        player.sendMessage(prefix + tpMsg);
                    } else {
                        findAndTeleport(player, nextAttempt);
                    }
                });
            });
        });
    }

    /**
     * Determines whether the column at (x, z) has safe solid ground.
     *
     * @return The Y-coordinate of the safe block, or -1 if unsafe
     */
    private int getSafeGroundY(World world, int x, int z) {
        int maxY = world.getMaxHeight() - 60;
        int minY = world.getMinHeight();

        for (int y = maxY; y >= minY + 10; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material mat = block.getType();

            if (!mat.isAir()) {
                if (blacklistedMaterials.contains(mat)) {
                    return -1; // Unsafe surface (liquid, hazard, etc.)
                }

                // Check if block is solid and safe to walk on
                if (mat.isSolid()) {
                    // Check head and feet spaces
                    Block feet = world.getBlockAt(x, y + 1, z);
                    Block head = world.getBlockAt(x, y + 2, z);
                    if (feet.getType().isAir() && head.getType().isAir()) {
                        return y + 1; // Safe standing surface
                    }
                }
            }
        }
        return -1;
    }

    public void removePlayer(UUID uuid) {
        teleportingPlayers.remove(uuid);
    }
}
