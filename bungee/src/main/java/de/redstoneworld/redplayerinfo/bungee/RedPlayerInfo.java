package de.redstoneworld.redplayerinfo.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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

import java.io.IOException;
import java.util.logging.Level;


public final class RedPlayerInfo extends BungeePlugin {

    public static final String PLUGIN_MESSAGE_CHANNEL = "RedPlayerInfo";

    private PlayerInfoStorage storage;

    @Override
    public void onEnable() {
        registerCommand("redafk", RedAfkCommand.class);
        registerCommand("redplayerinfo", RedPlayerInfoCommand.class);
        registerCommand("redwhosafk", RedWhosAfkCommand.class);
        getProxy().registerChannel(PLUGIN_MESSAGE_CHANNEL);
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

        if (!load()) {
            getLogger().log(Level.SEVERE, "Error while enabling the plugin!");
        }
        
        if (getProxy().getPluginManager().getPlugin("BungeeTabListPlus") != null) {
            new AfkPlaceholderVariable(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.destroy();
        }
    }

    public boolean load() {
        try {
            getConfig().loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading the config file!", e);
            return false;
        }
        if (storage != null) {
            storage.destroy();
        }
        try {
            storage = new MysqlStorage(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while initializing MySQL storage. Using only the cache now. (" + e.getMessage() + ")");
            storage = new CachedStorage(this);
            return false;
        }
        return true;
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

    /**
     * Set a player afk
     * @param proxiedPlayer The player to set
     * @param reason        The reason why he is afk
     * @param manual        Whether or not he set this status imself via the command
     */
    public void setAfk(ProxiedPlayer proxiedPlayer, String reason, boolean manual) {
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

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("setafk");
        out.writeLong(player.getUniqueId().getMostSignificantBits());
        out.writeLong(player.getUniqueId().getLeastSignificantBits());
        out.writeBoolean(manual);
        proxiedPlayer.getServer().sendData(PLUGIN_MESSAGE_CHANNEL, out.toByteArray());

        getStorage().savePlayer(player);
    }
}
