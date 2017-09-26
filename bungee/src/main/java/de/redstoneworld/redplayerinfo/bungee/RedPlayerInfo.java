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
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.caching.MetaData;
import net.alpenblock.bungeeperms.BungeePerms;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;


public final class RedPlayerInfo extends BungeePlugin {

    public static final String PLUGIN_MESSAGE_CHANNEL = "RedPlayerInfo";

    private PlayerInfoStorage storage;

    private BungeePerms bungeePerms;
    private LuckPermsApi luckPermsApi;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new RedAfkCommand(this, "redafk"));
        getProxy().getPluginManager().registerCommand(this, new RedPlayerInfoCommand(this, "redplayerinfo"));
        getProxy().getPluginManager().registerCommand(this, new RedWhosAfkCommand(this, "redwhosafk"));
        getProxy().registerChannel(PLUGIN_MESSAGE_CHANNEL);
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

        if (!load()) {
            getLogger().log(Level.SEVERE, "Error while enabling the plugin!");
        }
        
        if (getProxy().getPluginManager().getPlugin("BungeeTabListPlus") != null) {
            new AfkPlaceholderVariable(this).register();
        }

        if (getProxy().getPluginManager().getPlugin("BungeePerms") != null) {
            bungeePerms = BungeePerms.getInstance();
            getLogger().log(Level.INFO, "Detected BungeePerms " + bungeePerms.getPlugin().getVersion());
        }

        if (getProxy().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsApi = LuckPerms.getApi();
            getLogger().log(Level.INFO, "Detected LuckPerms " + luckPermsApi.getVersion());
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.destroy();
        }
    }

    public boolean load() {
        boolean error = false;
        try {
            getConfig().loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading the config file!", e);
            error = true;
        }
        Set<RedPlayer> currentPlayers = new HashSet<>();
        if (storage != null) {
            currentPlayers.addAll(storage.getCachedPlayers());
            storage.destroy();
        }
        try {
            storage = new MysqlStorage(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while initializing MySQL storage. Using only the cache now. (" + e.getMessage() + ")");
            storage = new CachedStorage(this);
            error = true;
        }
        for (RedPlayer player : currentPlayers) {
            storage.savePlayer(player);
        }
        return !error;
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
        logDebug(proxiedPlayer.getName() + "/" + proxiedPlayer.getUniqueId() + " is not afk anymore");
        RedPlayer player = getPlayer(proxiedPlayer);
        if (player.isAfk()) {
            player.unsetAfk();
            if (getConfig().getBoolean("messages.public-broadcast")) {
                broadcast("rwm.redafk.afk-use", getConfig().getString("messages.no-afk"), "player", player.getName());
            } else {
                proxiedPlayer.sendMessage(translate(getConfig().getString("messages.unset-afk")));
            }
            getStorage().savePlayer(player);
            sendAfkInfo(proxiedPlayer);
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
        logDebug(proxiedPlayer.getName() + "/" + proxiedPlayer.getUniqueId() + " is now afk");
        RedPlayer player = getPlayer(proxiedPlayer);
        player.setAfk(reason, manual);
        if (getConfig().getBoolean("messages.public-broadcast")) {
            broadcast("rwm.redafk.afk-use", getConfig().getString("messages.is-afk"),
                    "player", player.getName(),
                    "reason", reason.isEmpty()
                            ? getConfig().getString("messages.afk-with-no-reason")
                            : translate(getConfig().getString("messages.reason"), "message", reason)
            );
        } else {
            proxiedPlayer.sendMessage(translate(getConfig().getString("messages.set-afk"),
                    "reason", translate(getConfig().getString("messages.reason"),
                            "message",reason.isEmpty()
                                    ? getConfig().getString("messages.afk-with-no-reason")
                                    : translate(getConfig().getString("messages.reason"), "message", reason)))
            );
        }
        getStorage().savePlayer(player);
        sendAfkInfo(proxiedPlayer);
    }

    public void sendAfkInfo(ProxiedPlayer proxiedPlayer) {
        RedPlayer player = getPlayer(proxiedPlayer);
        if (player.isAfk()) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("setafk");
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            out.writeBoolean(!player.isAutoAfk());
            proxiedPlayer.getServer().sendData(PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
        } else {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("unsetafk");
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            proxiedPlayer.getServer().sendData(PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
        }
    }

    public String getPrefix(RedPlayer player) {
        if (bungeePerms != null) {
            net.alpenblock.bungeeperms.User user = bungeePerms.getPermissionsManager().getUser(player.getUniqueId(), true);
            if (user != null) {
                return ChatColor.translateAlternateColorCodes('&', user.buildPrefix());
            }
        }
        if (luckPermsApi != null) {
            MetaData metaData = getMetaData(player);
            if (metaData != null && metaData.getPrefix() != null) {
                return ChatColor.translateAlternateColorCodes('&', metaData.getPrefix());
            }
        }
        return "";
    }

    public String getSuffix(RedPlayer player) {
        if (bungeePerms != null) {
            net.alpenblock.bungeeperms.User user = bungeePerms.getPermissionsManager().getUser(player.getUniqueId(), true);
            if (user != null) {
                return ChatColor.translateAlternateColorCodes('&', user.buildSuffix());
            }
        }
        if (luckPermsApi != null) {
            MetaData metaData = getMetaData(player);
            if (metaData != null && metaData.getSuffix() != null) {
                return ChatColor.translateAlternateColorCodes('&', metaData.getSuffix());
            }
        }
        return "";
    }

    private MetaData getMetaData(RedPlayer player) {
        me.lucko.luckperms.api.User lpUser = luckPermsApi.getUser(player.getUniqueId());
        if (lpUser != null) {
            Contexts contexts = luckPermsApi.getContextForUser(lpUser).orElse(null);
            if (contexts != null) {
                return lpUser.getCachedData().getMetaData(contexts);
            }
        }
        return null;
    }

    public String getGroup(RedPlayer player) {
        String groupName = "";
        if (bungeePerms != null) {
            net.alpenblock.bungeeperms.User user = bungeePerms.getPermissionsManager().getUser(player.getUniqueId(), true);
            if (user != null) {
                int rank = Integer.MAX_VALUE;
                for (net.alpenblock.bungeeperms.Group group : user.getGroups()) {
                    if (group.getRank() < rank) {
                        groupName = group.getName();
                    }
                }
            }
        }
        if (luckPermsApi != null) {
            if (!luckPermsApi.isUserLoaded(player.getUniqueId())) {
                CompletableFuture<Boolean> future = luckPermsApi.getStorage().loadUser(player.getUniqueId(), player.getName());
                // We are already on our own thread so we just pause it as there is
                // no way of loading the user on the same thread in the LuckPermsAPI
                // Times out after five seconds
                long start = System.currentTimeMillis();
                while (!future.isDone() && System.currentTimeMillis() < start + 5000) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) { }
                }
            }
            me.lucko.luckperms.api.User lpUser = luckPermsApi.getUser(player.getUniqueId());
            if (lpUser != null) {
                int weight = Integer.MIN_VALUE;
                for (me.lucko.luckperms.api.Group group : luckPermsApi.getGroups()) {
                    if (group.getWeight().orElse(weight) > weight
                            && lpUser.hasPermission(luckPermsApi.buildNode("group." + group.getName()).setValue(true).build()).asBoolean()) {
                        groupName = group.getFriendlyName();
                    }
                }
            }
        }
        return groupName;
    }

    public void logDebug(String message) {
        if (getConfig().getBoolean("debug")) {
            getLogger().log(Level.INFO, message);
        }
    }
}
