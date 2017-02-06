package de.redstoneworld.redplayerinfo.bungee.storages;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;

import java.util.UUID;

public interface PlayerInfoStorage {

    /**
     * Save a player
     * @param player The player to save
     */
    public void savePlayer(RedPlayer player);

    /**
     * Get a player
     * @param playerName The name of the player
     * @return The player's RedPlayer object or null if he is unknown
     */
    public RedPlayer getPlayer(String playerName);

    /**
     * Get a player
     * @param playerId The UUID of the player
     * @return The player's RedPlayer object or null if he is unknown
     */
    public RedPlayer getPlayer(UUID playerId);

    /**
     * Called when the plugin is stopped or reloaded
     */
    void destroy();
}
