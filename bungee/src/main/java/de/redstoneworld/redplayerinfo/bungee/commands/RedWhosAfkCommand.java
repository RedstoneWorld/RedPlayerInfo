package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

public class RedWhosAfkCommand extends PluginCommand {

    public RedWhosAfkCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
    }

    @Override
    public boolean run(CommandSender sender, String[] args) {
        List<String> players = new ArrayList<>();
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            RedPlayer redPlayer = ((RedPlayerInfo) plugin).getPlayer(player);
            if (redPlayer.isAfk()) {
                players.add(redPlayer.getName());
            }
        }

        if (players.size() > 0) {
            StringBuilder playersString = new StringBuilder(players.get(0));
            for (int i = 1; i < players.size(); i++) {
                playersString.append(", ").append(players.get(i));
            }

            sender.sendMessage(BungeePlugin.translate(plugin.getConfig().getString("messages.whos-afk"), "players", playersString.toString()));
        } else {
            sender.sendMessage(BungeePlugin.translate(plugin.getConfig().getString("messages.noone-is-afk")));
        }
        return true;
    }
}
