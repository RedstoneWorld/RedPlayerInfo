package de.redstoneworld.redplayerinfo.bungee.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Level;

public class PluginMessageListener implements Listener {
    private RedPlayerInfo plugin;

    public PluginMessageListener(RedPlayerInfo plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(plugin.getDescription().getName())) {
            return;
        }

        if (!(event.getReceiver() instanceof ProxiedPlayer)) {
            // disallow plugin messages from the client
            String senderName = event.getSender().getAddress().toString();
            if (event.getSender() instanceof ProxiedPlayer) {
                senderName = ((ProxiedPlayer) event.getSender()).getName();
            }
            plugin.getLogger().log(Level.WARNING, senderName + " tried to send plugin message on " + event.getTag() + " channel!");
            event.setCancelled(true);
            return;
        }

        ProxiedPlayer receiver = (ProxiedPlayer) event.getReceiver();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();
        if ("afktime".equals(subChannel)) {
            if (plugin.getConfig().getBoolean("auto-afk.enabled")) {
                int seconds = in.readInt();
                RedPlayer player = plugin.getPlayer(receiver);
                if (!player.isAfk()) {
                    if (seconds * 60 >= plugin.getConfig().getInt("afk-time")) {
                        plugin.setAfk(receiver, plugin.translate(plugin.getConfig().getString("messages.auto-afk")));
                    } else if (plugin.getConfig().getBoolean("auto-warning.enabled") && seconds * 60 >= plugin.getConfig().getInt("auto-warning")) {
                        receiver.sendMessage(plugin.translate(plugin.getConfig().getString("messages.auto-warning")));
                    }
                }
            }
        } else if ("unsetafk".equals(subChannel)) {
            plugin.unsetAfk(receiver);
        } else {
            plugin.getLogger().log(Level.WARNING, subChannel + " is an unknown subchannel on " + event.getTag() + " channel!");
        }
    }
}
