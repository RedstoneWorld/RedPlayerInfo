package de.redstoneworld.redplayerinfo.bungee.listeners;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerListener implements Listener {

    private final RedPlayerInfo plugin;

    public PlayerListener(RedPlayerInfo plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        RedPlayer player = new RedPlayer(event.getPlayer());
        player.setLoginTime(System.currentTimeMillis());
        plugin.getStorage().savePlayer(player);
    }

    @EventHandler
    public void onPlayerLogout(PlayerDisconnectEvent event) {
        RedPlayer player = plugin.getStorage().getPlayer(event.getPlayer().getUniqueId());
        if (player != null) {
            player.setLogoutTime(System.currentTimeMillis());
            player.unsetAfk();
            plugin.getStorage().savePlayer(player);
        }
    }
}
