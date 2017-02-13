package de.redstoneworld.redplayerinfo.bungee;

import codecrafter47.bungeetablistplus.api.bungee.BungeeTabListPlusAPI;
import codecrafter47.bungeetablistplus.api.bungee.Variable;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.logging.Level;

public class AfkPlaceholderVariable extends Variable {
    private final RedPlayerInfo plugin;

    public AfkPlaceholderVariable(RedPlayerInfo plugin) {
        super("red_afk");
        this.plugin = plugin;
    }

    @Override
    public String getReplacement(ProxiedPlayer player) {
        return String.valueOf(plugin.getPlayer(player).isAfk());
    }

    public void register() {
        BungeeTabListPlusAPI.registerVariable(plugin, this);
        plugin.getLogger().log(Level.INFO, "Registered AfkPlaceholderVariable");
    }
}
