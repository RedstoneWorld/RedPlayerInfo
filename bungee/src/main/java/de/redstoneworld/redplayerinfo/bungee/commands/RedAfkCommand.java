package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class RedAfkCommand extends PluginCommand {

    public RedAfkCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
    }

    @Override
    public boolean run(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            return true;
        }

        if (args.length == 0 && ((RedPlayerInfo) plugin).unsetAfk((ProxiedPlayer) sender)) {
            return true;
        } else if (args.length > 0){
            StringBuilder reason = new StringBuilder(args[0]);
            for(int i = 1; i < args.length; i++) {
                reason.append(" ").append(args[i]);
            }
            ((RedPlayerInfo) plugin).setAfk((ProxiedPlayer) sender, reason.toString());
            return true;
        }
        return false;
    }
}
