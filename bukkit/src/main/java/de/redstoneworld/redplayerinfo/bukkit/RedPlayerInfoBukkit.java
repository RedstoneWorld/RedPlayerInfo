package de.redstoneworld.redplayerinfo.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedPlayerInfoBukkit extends JavaPlugin {

    private static final String PLUGIN_MESSAGE_CHANNEL = "RedPlayerInfo";

    private Map<UUID, Long> lastActivity = new HashMap<>();

    private Map<UUID, Boolean> afkPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, PLUGIN_MESSAGE_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, PLUGIN_MESSAGE_CHANNEL, (channel, player, message) -> {
            if (!PLUGIN_MESSAGE_CHANNEL.equals(channel)) {
                return;
            }
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            if ("setafk".equals(subChannel)) {
                afkPlayers.put(new UUID(in.readLong(), in.readLong()), in.readBoolean());
            } else if ("unsetafk".equals(subChannel)) {
                afkPlayers.remove(new UUID(in.readLong(), in.readLong()));
            }
        });
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getScheduler().runTaskTimer(this, this::updateStatus, 20 * 60, 20 * 10);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        lastActivity.clear();
        lastActivity = null;
    }

    /**
     * Set the last activity of a player to the current time
     * @param player    The Player to set
     */
    public void updateLastActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (isAutoAfk(player.getUniqueId())) {
            unsetAfk(player);
        }
    }

    private void unsetAfk(Player player) {
        afkPlayers.remove(player.getUniqueId());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("unsetafk");
        player.sendPluginMessage(this, PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
    }

    /**
     * Check whether or not a player is marked as afk
     * @param playerId  The UUID of the player
     * @return          true if the player was marked as afk, false if not
     */
    public boolean isAfk(UUID playerId) {
        return afkPlayers.containsKey(playerId);
    }

    /**
     * Check whether or not a player was automatically marked as afk
     * @param playerId  The UUID of the player
     * @return          true if the player was automatically marked as afk,
     *                  false if he set it himself via the command
     */
    public boolean isAutoAfk(UUID playerId) {
        return isAfk(playerId) && !afkPlayers.get(playerId);
    }

    /**
     * Get the time in milliseconds when a player was last active
     * @param player    The Player to get the last activity for
     * @return          The timestamp he was last active or -1 if he never was active/online
     */
    public long getLastActivity(Player player) {
        if (lastActivity.containsKey(player.getUniqueId())) {
            return lastActivity.get(player.getUniqueId());
        }
        return -1;
    }

    /**
     * Remove a player from the last activity tracking e.g. when he logs out
     * @param player    The Player to remove
     */
    public void removePlayer(Player player) {
        lastActivity.remove(player.getUniqueId());
        afkPlayers.remove(player.getUniqueId());
    }

    /**
     * Send all the activity status to the Bungee
     */
    private void updateStatus() {
        if (!getServer().getOnlinePlayers().isEmpty() && lastActivity.size() - afkPlayers.size() > 0) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("afktime");
            out.writeInt(lastActivity.size() - afkPlayers.size());
            for (Map.Entry<UUID, Long> entry : lastActivity.entrySet()) {
                if (!isAfk(entry.getKey())) {
                    out.writeLong(entry.getKey().getMostSignificantBits());
                    out.writeLong(entry.getKey().getLeastSignificantBits());
                    out.writeInt((int) ((System.currentTimeMillis() - entry.getValue()) / 1000));
                }
            }
            getServer().getOnlinePlayers().iterator().next().sendPluginMessage(this, PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
        }
    }
}
