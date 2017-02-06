package de.redstoneworld.redplayerinfo.bungee;

import codecrafter47.bungeetablistplus.api.bungee.Variable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AfkPlaceholderVariable extends Variable {
    private final RedPlayerInfo plugin;

    public AfkPlaceholderVariable(RedPlayerInfo plugin) {
        super("afk");
        this.plugin = plugin;
    }

    @Override
    public String getReplacement(ProxiedPlayer player) {
        RedPlayer redPlayer = plugin.getPlayer(player);
        if (redPlayer.isAfk()) {
            return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("custom-placeholder.afk-style.ON"));
        }
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("custom-placeholder.afk-style.OFF"));
    }
}
