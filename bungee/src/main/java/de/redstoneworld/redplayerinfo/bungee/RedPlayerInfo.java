package de.redstoneworld.redplayerinfo.bungee;

import de.redstoneworld.redplayerinfo.bungee.commands.RedAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedPlayerInfoCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedWhosAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.listeners.PlayerListener;
import de.redstoneworld.redplayerinfo.bungee.storages.CachedStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.MysqlStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.PlayerInfoStorage;
import de.themoep.bungeeplugin.BungeePlugin;

import java.sql.SQLException;
import java.util.logging.Level;


public final class RedPlayerInfo extends BungeePlugin {

    private PlayerInfoStorage storage;

    @Override
    public void onEnable() {
        registerCommand("redafk", RedAfkCommand.class);
        registerCommand("redplayerinfo", RedPlayerInfoCommand.class);
        registerCommand("redwhosafk", RedWhosAfkCommand.class);
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
        try {
            storage = new MysqlStorage(this);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error while initializing MySQL storage. Using only the cache now.", e);
            storage = new CachedStorage(this);
        }
    }


    public PlayerInfoStorage getStorage() {
        return storage;
    }
}
