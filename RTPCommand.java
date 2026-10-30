package com.rtpzone.command;

import com.rtpzone.RTPZonePlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles /rtp command execution and subcommands like /rtp zone select.
 */
public class RTPCommand implements CommandExecutor, TabCompleter {

    private final RTPZonePlugin plugin;

    public RTPCommand(RTPZonePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§b§lRTP§3§lZone§8] ");

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        // Subcommand: /rtp zone [select | remove]
        if (args[0].equalsIgnoreCase("zone")) {
            if (!sender.hasPermission("rtpzone.admin")) {
                sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cYou do not have permission."));
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can execute zone selection commands in-game.");
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("select")) {
                // Exact command: /rtp zone select
                Location createdLoc = plugin.getZoneManager().createHoleZone(player);
                String msg = plugin.getConfig().getString("messages.zone-selected",
                                "§aSuccessfully selected and created the RTP hole area at §e%x%, %y%, %z%§a!")
                        .replace("%x%", String.valueOf(createdLoc.getBlockX()))
                        .replace("%y%", String.valueOf(createdLoc.getBlockY()))
                        .replace("%z%", String.valueOf(createdLoc.getBlockZ()));
                player.sendMessage(prefix + msg);
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                plugin.getZoneManager().removeZone();
                player.sendMessage(prefix + plugin.getConfig().getString("messages.zone-removed", "§cRTP hole area has been removed."));
                return true;
            } else {
                sender.sendMessage(prefix + "§eUsage: §f/" + label + " zone select §7or §f/" + label + " zone remove");
                return true;
            }
        }

        // Subcommand: /rtp reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rtpzone.admin")) {
                sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cYou do not have permission."));
                return true;
            }
            plugin.reloadConfig();
            plugin.getZoneManager().loadFromConfig();
            plugin.getRtpManager().loadBlacklist();
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.reloaded", "§aConfiguration reloaded successfully!"));
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§8§m----------------§r §b§lRTPZone Help §8§m----------------");
        sender.sendMessage("§b/" + label + " zone select §7- Creates the RTP hole area in front of you");
        sender.sendMessage("§b/" + label + " zone remove §7- Removes the active RTP hole area");
        sender.sendMessage("§b/" + label + " reload      §7- Reloads the plugin configuration");
        sender.sendMessage("§b/" + label + " help        §7- Shows this help menu");
        sender.sendMessage("§8§m---------------------------------------------");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> sub = Arrays.asList("zone", "reload", "help");
            List<String> matches = new ArrayList<>();
            for (String s : sub) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    matches.add(s);
                }
            }
            return matches;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("zone")) {
            List<String> zoneSub = Arrays.asList("select", "remove");
            List<String> matches = new ArrayList<>();
            for (String s : zoneSub) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    matches.add(s);
                }
            }
            return matches;
        }

        return Collections.emptyList();
    }
}
