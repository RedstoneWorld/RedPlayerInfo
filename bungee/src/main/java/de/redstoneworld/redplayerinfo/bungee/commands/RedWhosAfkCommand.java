package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

public class RedWhosAfkCommand extends PluginCommand<RedPlayerInfo> {

    public RedWhosAfkCommand(RedPlayerInfo plugin, String name) {
        super(plugin, name);
    }

    @Override
    public boolean run(CommandSender sender, String[] args) {
        List<String> players = new ArrayList<>();
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            RedPlayer redPlayer = plugin.getPlayer(player);
            if (redPlayer.isAfk()) {
                players.add(redPlayer.getName());
            }
        }

        if (players.size() > 0) {
            StringBuilder playersString = new StringBuilder(players.get(0));
            for (int i = 1; i < players.size(); i++) {
                playersString.append(", ").append(players.get(i));
            }

            sender.sendMessage(RedPlayerInfo.translate(plugin.getConfig().getString("messages.whos-afk"), "players", playersString.toString()));
        } else {
            sender.sendMessage(RedPlayerInfo.translate(plugin.getConfig().getString("messages.noone-is-afk")));
        }
        return true;
    }
}
