package de.redstoneworld.redplayerinfo.bungee.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.logging.Level;

public class PluginMessageListener implements Listener {
    private RedPlayerInfo plugin;

    public PluginMessageListener(RedPlayerInfo plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().startsWith(RedPlayerInfo.PLUGIN_MESSAGE_CHANNEL)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getReceiver() instanceof ProxiedPlayer)) {
            // disallow plugin messages from the client
            String senderName = event.getSender().getAddress().toString();
            if (event.getSender() instanceof ProxiedPlayer) {
                senderName = ((ProxiedPlayer) event.getSender()).getName();
            }
            plugin.getLogger().log(Level.WARNING, senderName + " tried to send plugin message on " + event.getTag() + " channel!");
            return;
        }

        ProxiedPlayer receiver = (ProxiedPlayer) event.getReceiver();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = event.getTag().split(":", 2)[1];
        if ("afktime".equals(subChannel)) {
            plugin.logDebug("Received afktime plugin message");
            if (plugin.getConfig().getBoolean("auto-afk.enabled")) {
                int amount = in.readInt();
                for (int i = 0; i < amount; i++) {
                    UUID playerId = new UUID(in.readLong(), in.readLong());
                    long seconds = in.readLong();

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
                    if (player == null || player.hasPermission("rwm.redafk.afk-immune")) {
                        continue;
                    } else if (player.getServer().getInfo() != ((Server) event.getSender()).getInfo()) {
                        // ignore messages sent by a server that the player isn't even on
                        continue;
                    }
                    plugin.logDebug(player.getName() + "/" + player.getUniqueId() + " is afk for " + seconds + " seconds");

                    RedPlayer redPlayer = plugin.getPlayer(player);
                    if (!redPlayer.isAfk()) {
                        int afkTime = plugin.getConfig().getInt("auto-afk.time");
                        int warningTime = plugin.getConfig().getInt("auto-warning.time");
                        if (seconds >= afkTime) {
                            plugin.setAfk(player, plugin.translate(plugin.getConfig().getString("messages.auto-afk")), false);
                        } else if (plugin.getConfig().getBoolean("auto-warning.enabled") && seconds >= warningTime && seconds < warningTime + 10) {
                            player.sendMessage(plugin.translate(plugin.getConfig().getString("messages.auto-warning")));
                        }
                    }
                }
            }
        } else if ("unsetafk".equals(subChannel)) {
            plugin.logDebug(receiver.getName() + " received unsetafk plugin message");
            boolean manual = in.readBoolean();
            if (!manual || plugin.getConfig().getBoolean("unset-manual-afk-on-activity")) {
                plugin.unsetAfk(receiver);
            }
        } else {
            plugin.getLogger().log(Level.WARNING, subChannel + " is an unknown subchannel on " + event.getTag() + " channel!");
        }
    }
}
