package com.rtpzone;

import com.rtpzone.camera.CameraManager;
import com.rtpzone.command.RTPCommand;
import com.rtpzone.listener.LandingListener;
import com.rtpzone.listener.ZoneListener;
import com.rtpzone.teleport.RTPManager;
import com.rtpzone.zone.ZoneManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class RTPZonePlugin extends JavaPlugin {

    private ZoneManager zoneManager;
    private CameraManager cameraManager;
    private RTPManager rtpManager;

    @Override
    public void onEnable() {
        // Save default config.yml if not existing
        saveDefaultConfig();

        // Initialize managers
        this.cameraManager = new CameraManager(this);
        this.zoneManager = new ZoneManager(this);
        this.rtpManager = new RTPManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new LandingListener(this), this);

        // Register commands & tab completers
        RTPCommand rtpCommand = new RTPCommand(this);
        PluginCommand command = getCommand("rtp");
        if (command != null) {
            command.setExecutor(rtpCommand);
            command.setTabCompleter(rtpCommand);
        }

        getLogger().info("RTPZone 1.0.0 enabled successfully for Paper 1.21!");
    }

    @Override
    public void onDisable() {
        if (this.cameraManager != null) {
            this.cameraManager.shutdown();
        }
        if (this.zoneManager != null) {
            this.zoneManager.cancelParticleTask();
        }
        getLogger().info("RTPZone 1.0.0 disabled.");
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public RTPManager getRtpManager() {
        return rtpManager;
    }
}
