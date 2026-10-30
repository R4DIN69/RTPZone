package com.rtpzone.listener;

import com.rtpzone.RTPZonePlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for descending players touching the ground to trigger
 * the automatic camera POV transition back to 1st person mode.
 */
public class LandingListener implements Listener {

    private final RTPZonePlugin plugin;

    public LandingListener(RTPZonePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getCameraManager().isDescending(player)) {
            return;
        }

        // Check if player has touched ground
        boolean onGround = player.isOnGround();

        if (!onGround) {
            // Also check if block directly under player's feet is solid
            Location loc = player.getLocation();
            Block blockBelow = loc.clone().subtract(0, 0.15, 0).getBlock();
            if (blockBelow.getType().isSolid()) {
                onGround = true;
            }
        }

        if (onGround) {
            plugin.getCameraManager().handleLanding(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.getCameraManager().isDescending(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                // Cancel fall damage during descent
                event.setCancelled(true);
                // Also trigger ground landing
                plugin.getCameraManager().handleLanding(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCameraManager().cleanupPlayer(event.getPlayer());
        plugin.getRtpManager().removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getCameraManager().cleanupPlayer(event.getPlayer());
        plugin.getRtpManager().removePlayer(event.getPlayer().getUniqueId());
    }
}
