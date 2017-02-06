package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
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

        RedPlayer player = ((RedPlayerInfo) plugin).getPlayer((ProxiedPlayer) sender);
        if (args.length == 0 && player.isAfk()) {
            player.unsetAfk();
            if (plugin.getConfig().getBoolean("messages.public-broadcast")) {
                plugin.broadcast(getPermission(), plugin.getConfig().getString("messages.no-afk"), "player", player.getName());
            } else {
                sender.sendMessage(plugin.translate(plugin.getConfig().getString("messages.unset-afk")));
            }
        } else if (args.length > 0){
            StringBuilder reason = new StringBuilder(args[0]);
            for(int i = 1; i < args.length; i++) {
                reason.append(" ").append(args[i]);
            }
            player.setAfk(reason.toString());
            if (plugin.getConfig().getBoolean("messages.public-broadcast")) {
                plugin.broadcast(getPermission(), plugin.getConfig().getString("messages.is-afk"),
                        "player", player.getName(),
                        "reason", plugin.translate(plugin.getConfig().getString("messages.reason"), "message", reason.toString())
                );
            } else {
                sender.sendMessage(plugin.translate(plugin.getConfig().getString("messages.set-afk"),
                        "reason", plugin.translate(plugin.getConfig().getString("messages.reason"), "message", reason.toString()))
                );
            }
        } else {
            return false;
        }
        ((RedPlayerInfo) plugin).getStorage().savePlayer(player);
        return true;
    }
}
