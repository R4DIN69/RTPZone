package com.rtpzone.camera;

import com.rtpzone.RTPZonePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles camera POV switching between 3rd person and 1st person,
 * descent effects, Bedrock / Geyser camera commands, and landing triggers.
 */
public class CameraManager {

    private final RTPZonePlugin plugin;
    // Set of players currently descending from the 50-block drop
    private final Set<UUID> descendingPlayers = ConcurrentHashMap.newKeySet();
    // Records the target ground Y coordinate for each descending player
    private final Map<UUID, Integer> targetGroundY = new ConcurrentHashMap<>();
    private BukkitTask descentTrackerTask;

    public CameraManager(RTPZonePlugin plugin) {
        this.plugin = plugin;
        startDescentTrackerTask();
    }

    /**
     * Activates 3rd-person POV mode and slow falling descent for a teleported player.
     *
     * @param player  The player descending
     * @param groundY The Y-coordinate of the safe ground beneath them
     */
    public void activateThirdPersonDescent(Player player, int groundY) {
        UUID uuid = player.getUniqueId();
        descendingPlayers.add(uuid);
        targetGroundY.put(uuid, groundY);

        // Bedrock / Geyser native camera command integration
        boolean bedrockSupport = plugin.getConfig().getBoolean("camera.bedrock-camera-support", true);
        if (bedrockSupport) {
            String preset = plugin.getConfig().getString("camera.bedrock-third-person-preset", "minecraft:third_person");
            // Run /camera <player> set <preset> via console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "camera " + player.getName() + " set " + preset);
        }

        // Java Edition on-screen title prompt (since vanilla Java F5 is client-authoritative)
        if (plugin.getConfig().getBoolean("camera.show-f5-prompt", true)) {
            String titleText = plugin.getConfig().getString("camera.freefall-title", "§b§l✦ FREEFALLING ✦");
            String subText = plugin.getConfig().getString("camera.freefall-subtitle", "§eSlow falling from 50 blocks! §7(Press F5 for 3rd person)");

            Title title = Title.title(
                    Component.text(titleText),
                    Component.text(subText),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))
            );
            player.showTitle(title);
        }

        // Sound effect
        player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.2f);
    }

    /**
     * Handles when the descending player touches the ground:
     * - Restores 1st person POV mode
     * - Clears Bedrock camera
     * - Removes slow falling
     * - Plays landing sound and shows landing title
     */
    public void handleLanding(Player player) {
        UUID uuid = player.getUniqueId();
        if (!descendingPlayers.contains(uuid)) {
            return;
        }

        descendingPlayers.remove(uuid);
        targetGroundY.remove(uuid);

        // Remove slow falling effect once safely landed
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);

        // Restore camera POV to 1st person mode (clears /camera for Bedrock/Geyser)
        boolean bedrockSupport = plugin.getConfig().getBoolean("camera.bedrock-camera-support", true);
        if (bedrockSupport) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "camera " + player.getName() + " clear");
        }

        // Show landing confirmation title
        String landingTitle = plugin.getConfig().getString("camera.landing-title", "§a§l✔ SAFE LANDING!");
        String landingSub = plugin.getConfig().getString("camera.landing-subtitle", "§fPOV returned to first person. Welcome to the wild!");

        Title title = Title.title(
                Component.text(landingTitle),
                Component.text(landingSub),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))
        );
        player.showTitle(title);

        // Landing sound
        String soundName = plugin.getConfig().getString("camera.landing-sound", "ENTITY_PLAYER_LEVELUP");
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Visual landing puff particles
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.CLOUD, loc.getX(), loc.getY() + 0.1, loc.getZ(), 20, 0.6, 0.2, 0.6, 0.05);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc.getX(), loc.getY() + 0.5, loc.getZ(), 15, 0.4, 0.4, 0.4, 0.08);
    }

    /**
     * Checks if a player is currently in the 50-block slow falling descent.
     */
    public boolean isDescending(Player player) {
        return descendingPlayers.contains(player.getUniqueId());
    }

    /**
     * Cleans up player tracking on disconnect or death.
     */
    public void cleanupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (descendingPlayers.remove(uuid)) {
            targetGroundY.remove(uuid);
            // Clear Bedrock camera if was active
            boolean bedrockSupport = plugin.getConfig().getBoolean("camera.bedrock-camera-support", true);
            if (bedrockSupport) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "camera " + player.getName() + " clear");
            }
        }
    }

    /**
     * Recurring task: spawns atmospheric particles around descending players
     * and shows an altitude action bar.
     */
    private void startDescentTrackerTask() {
        descentTrackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : descendingPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    descendingPlayers.remove(uuid);
                    targetGroundY.remove(uuid);
                    continue;
                }

                Location loc = player.getLocation();
                // Spawn wind and vortex trail particles
                loc.getWorld().spawnParticle(Particle.CLOUD, loc.getX(), loc.getY(), loc.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.getX(), loc.getY() + 0.5, loc.getZ(), 1, 0.2, 0.2, 0.2, 0.01);

                // Action bar altitude counter
                Integer groundY = targetGroundY.get(uuid);
                if (groundY != null) {
                    int distanceLeft = Math.max(0, loc.getBlockY() - groundY);
                    player.sendActionBar(Component.text(
                            "§e☁ Descending... §b" + distanceLeft + " blocks §eto ground §7| §eF5 for 3rd Person"
                    ));
                }
            }
        }, 0L, 5L);
    }

    public void shutdown() {
        if (descentTrackerTask != null && !descentTrackerTask.isCancelled()) {
            descentTrackerTask.cancel();
        }
        // Restore all players
        for (UUID uuid : descendingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                handleLanding(p);
            }
        }
        descendingPlayers.clear();
        targetGroundY.clear();
    }
}
