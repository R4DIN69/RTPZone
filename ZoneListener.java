package com.rtpzone.listener;

import com.rtpzone.RTPZonePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listens for players walking or falling into the configured RTP hole area.
 */
public class ZoneListener implements Listener {

    private final RTPZonePlugin plugin;

    public ZoneListener(RTPZonePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Performance guard: only check when crossing block coordinates
        if (to == null || (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();

        // Permission check
        if (!player.hasPermission("rtpzone.use")) {
            return;
        }

        // Check if player entered the hole zone
        if (plugin.getZoneManager().isInsideZone(to)) {
            plugin.getRtpManager().initiateHoleTeleport(player);
        }
    }
}
