package de.redstoneworld.redplayerinfo.bungee;

import de.redstoneworld.redplayerinfo.bungee.commands.RedAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedPlayerInfoCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedWhosAfkCommand;
import de.themoep.bungeeplugin.BungeePlugin;

public final class RedPlayerInfo extends BungeePlugin {

    @Override
    public void onEnable() {
        registerCommand("redafk", RedAfkCommand.class);
        registerCommand("redplayerinfo", RedPlayerInfoCommand.class);
        registerCommand("redwhosafk", RedWhosAfkCommand.class);
    }

}
