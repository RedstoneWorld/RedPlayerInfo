package de.redstoneworld.redplayerinfo.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.redstoneworld.redplayerinfo.bungee.commands.RedAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedPlayerListCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedPlayerInfoCommand;
import de.redstoneworld.redplayerinfo.bungee.commands.RedWhosAfkCommand;
import de.redstoneworld.redplayerinfo.bungee.listeners.PlayerListener;
import de.redstoneworld.redplayerinfo.bungee.listeners.PluginMessageListener;
import de.redstoneworld.redplayerinfo.bungee.storages.CachedStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.MysqlStorage;
import de.redstoneworld.redplayerinfo.bungee.storages.PlayerInfoStorage;
import de.themoep.bungeeplugin.BungeePlugin;
import net.alpenblock.bungeeperms.BungeePerms;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;


public final class RedPlayerInfo extends BungeePlugin {

    public static final String PLUGIN_MESSAGE_CHANNEL = "rpi:";

    private PlayerInfoStorage storage;

    private BungeePerms bungeePerms;
    private LuckPerms luckPermsApi;

    private RedPlayerListCommand redPlayerListCommand;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new RedAfkCommand(this, "redafk"));
        getProxy().getPluginManager().registerCommand(this, new RedPlayerInfoCommand(this, "redplayerinfo"));
        getProxy().getPluginManager().registerCommand(this, (redPlayerListCommand = new RedPlayerListCommand(this, "redplayerlist")));
        getProxy().getPluginManager().registerCommand(this, new RedWhosAfkCommand(this, "redwhosafk"));
        getProxy().registerChannel(PLUGIN_MESSAGE_CHANNEL + "setafk");
        getProxy().registerChannel(PLUGIN_MESSAGE_CHANNEL + "unsetafk");
        getProxy().registerChannel(PLUGIN_MESSAGE_CHANNEL + "afktime");
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
            luckPermsApi = LuckPermsProvider.get();
            getLogger().log(Level.INFO, "Detected LuckPerms " + luckPermsApi.getPluginMetadata().getVersion());
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
        redPlayerListCommand.load();
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
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            out.writeBoolean(!player.isAutoAfk());
            proxiedPlayer.getServer().sendData(PLUGIN_MESSAGE_CHANNEL + "setafk", out.toByteArray());
        } else {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeLong(player.getUniqueId().getMostSignificantBits());
            out.writeLong(player.getUniqueId().getLeastSignificantBits());
            proxiedPlayer.getServer().sendData(PLUGIN_MESSAGE_CHANNEL + "unsetafk", out.toByteArray());
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
            CachedMetaData metaData = getMetaData(player);
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
            CachedMetaData metaData = getMetaData(player);
            if (metaData != null && metaData.getSuffix() != null) {
                return ChatColor.translateAlternateColorCodes('&', metaData.getSuffix());
            }
        }
        return "";
    }

    private CachedMetaData getMetaData(RedPlayer player) {
        if (!luckPermsApi.getUserManager().isLoaded(player.getUniqueId())) {
            loadLuckPermsUser(player);
        }
        User lpUser = luckPermsApi.getUserManager().getUser(player.getUniqueId());
        if (lpUser != null) {
            ContextSet contexts = luckPermsApi.getContextManager().getContext(lpUser).orElse(null);
            if (contexts != null) {
                return lpUser.getCachedData().getMetaData(QueryOptions.contextual(contexts));
            }
        }
        return null;
    }

    public RedGroup getGroup(RedPlayer player) {
        RedGroup redGroup = null;
        if (bungeePerms != null) {
            net.alpenblock.bungeeperms.User user = bungeePerms.getPermissionsManager().getUser(player.getUniqueId(), true);
            if (user != null) {
                for (net.alpenblock.bungeeperms.Group group : user.getGroups()) {
                    if (redGroup == null || group.getRank() < redGroup.getRank()) {
                        redGroup = new RedGroup(group.getName(), group.getDisplay(), group.getPrefix(), group.getSuffix(), group.getRank());
                    }
                }
            }
        } else if (luckPermsApi != null) {
            if (!luckPermsApi.getUserManager().isLoaded(player.getUniqueId())) {
                loadLuckPermsUser(player);
            }
            net.luckperms.api.model.user.User lpUser = luckPermsApi.getUserManager().getUser(player.getUniqueId());
            if (lpUser != null) {

                Group group = luckPermsApi.getGroupManager().getGroup(lpUser.getPrimaryGroup());
                if (group != null) {
                    String groupName = group.getFriendlyName();
                    int bracket = groupName.indexOf('(');
                    if (bracket != -1 && groupName.endsWith(")")) {
                        groupName = groupName.substring(bracket + 1, groupName.length() - 1);
                    }
                    ContextSet contexts = luckPermsApi.getContextManager().getContext(lpUser).orElse(null);
                    String prefix = "";
                    String suffix = "";
                    if (contexts != null) {
                        CachedMetaData metaData = lpUser.getCachedData().getMetaData(QueryOptions.contextual(contexts));
                        prefix = metaData.getPrefix();
                        suffix = metaData.getSuffix();
                    }
                    redGroup = new RedGroup(group.getName(), groupName, prefix, suffix, -group.getWeight().orElse(0));
                }
            }
        }
        return redGroup != null ? redGroup : new RedGroup();
    }

    /**
     * Loads the user data from the database. This will block the current thread!
     * @param player The player to load
     */
    private void loadLuckPermsUser(RedPlayer player) {
        try {
            luckPermsApi.getUserManager().loadUser(player.getUniqueId(), player.getName()).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            getLogger().log(Level.WARNING, "Failed to load LuckPerms data of " + player.getName() + "/" + player.getUniqueId() + "! " + e.getMessage());
        }
    }

    public void logDebug(String message) {
        if (getConfig().getBoolean("debug")) {
            getLogger().log(Level.INFO, message);
        }
    }
}
