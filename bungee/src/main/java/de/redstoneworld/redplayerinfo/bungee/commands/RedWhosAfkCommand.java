package de.redstoneworld.redplayerinfo.bungee.commands;

import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.CommandSender;

public class RedWhosAfkCommand extends PluginCommand {

    public RedWhosAfkCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
    }

    @Override
    public boolean run(CommandSender sender, String[] args) {
        return false;
    }
}
