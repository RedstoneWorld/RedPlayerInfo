package de.redstoneworld.redplayerinfo.bungee;

import codecrafter47.bungeetablistplus.api.bungee.BungeeTabListPlusAPI;
import de.redstoneworld.redplayerinfo.bungee.commands.RedAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedPlayerInfoCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedWhosAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.listeners.PlayerListener;
import de.redstoneworld.redplayerinfo.bungee.listeners.PluginMessageListener;
import de.redstoneworld.redplayerinfo.bungee.storages.CachedStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.MysqlStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.PlayerInfoStorage;
import de.themoep.bungeeplugin.BungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.SQLException;
import java.util.logging.Level;


public final class RedPlayerInfo extends BungeePlugin {

    private PlayerInfoStorage storage;

    @Override
    public void onEnable() {
        registerCommand("redafk", RedAfkCommand.class);
        registerCommand("redplayerinfo", RedPlayerInfoCommand.class);
        registerCommand("redwhosafk", RedWhosAfkCommand.class);
        getProxy().registerChannel(getDescription().getName());
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

        if (getProxy().getPluginManager().getPlugin("BungeeTabListPlus") != null) {
            BungeeTabListPlusAPI.registerVariable(this, new AfkPlaceholderVariable(this));
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.destroy();
        }
    }

    public void load() {
        getConfig().loadConfig();
        if (storage != null) {
            storage.destroy();
        }
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

    /**
     * Get the RedPlayer object from a ProxiedPlayer
     * @param player    The proxied player
     * @return          The RedPlayer object from the storage or a new one
     */
    public RedPlayer getPlayer(ProxiedPlayer player) {
        RedPlayer redPlayer = getStorage().getPlayer(player.getUniqueId());
        if (redPlayer == null) {
            redPlayer = new RedPlayer(player);
        }
        return redPlayer;
    }

    /**
     * Unset the afk status of a player
     * @param proxiedPlayer The player that is no longer afk
     * @return              true if he was afk before, false if not
     */
    public boolean unsetAfk(ProxiedPlayer proxiedPlayer) {
        RedPlayer player = getPlayer(proxiedPlayer);
        if (player.isAfk()) {
            player.unsetAfk();
            if (getConfig().getBoolean("messages.public-broadcast")) {
                broadcast("rwm.redafk.afk-use", getConfig().getString("messages.no-afk"), "player", player.getName());
            } else {
                proxiedPlayer.sendMessage(translate(getConfig().getString("messages.unset-afk")));
            }
            getStorage().savePlayer(player);
            return true;
        }
        return false;
    }

    public void setAfk(ProxiedPlayer proxiedPlayer, String reason) {
        RedPlayer player = getPlayer(proxiedPlayer);
        player.setAfk(reason);
        if (getConfig().getBoolean("messages.public-broadcast")) {
            broadcast("rwm.redafk.afk-use", getConfig().getString("messages.is-afk"),
                    "player", player.getName(),
                    "reason", translate(getConfig().getString("messages.reason"), "message", reason)
            );
        } else {
            proxiedPlayer.sendMessage(translate(getConfig().getString("messages.set-afk"),
                    "reason", translate(getConfig().getString("messages.reason"), "message", reason))
            );
        }
        getStorage().savePlayer(player);
    }
}
