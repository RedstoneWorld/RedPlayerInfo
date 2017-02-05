package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.BungeePlugin;
import net.md_5.bungee.api.CommandSender;

public class RedPlayerInfoCommand extends PluginCommand {

    public RedPlayerInfoCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        return false;
    }


}
