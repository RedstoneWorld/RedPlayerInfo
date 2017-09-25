package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class RedAfkCommand extends PluginCommand<RedPlayerInfo> {

    public RedAfkCommand(RedPlayerInfo plugin, String name) {
        super(plugin, name);
    }

    @Override
    public boolean run(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            return true;
        }

        if (args.length == 0) {
            if (!plugin.unsetAfk((ProxiedPlayer) sender)) {
                plugin.setAfk((ProxiedPlayer) sender, "", true);
            }
            return true;
        }

        StringBuilder reason = new StringBuilder(args[0]);
        for(int i = 1; i < args.length; i++) {
            reason.append(" ").append(args[i]);
        }
        plugin.setAfk((ProxiedPlayer) sender, reason.toString(), true);
        return true;
    }
}
