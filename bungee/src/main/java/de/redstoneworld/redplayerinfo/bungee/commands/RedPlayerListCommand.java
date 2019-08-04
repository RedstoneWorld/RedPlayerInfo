package de.redstoneworld.redplayerinfo.bungee.commands;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.redstoneworld.redplayerinfo.bungee.RedGroup;
import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.PluginCommand;
import de.themoep.minedown.MineDown;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedPlayerListCommand extends PluginCommand<RedPlayerInfo> {

    private final Set<String> serverBlackList = new HashSet<>();
    private final Set<String> groupBlackList = new HashSet<>();
    private final Map<String, String> serverAliases = new HashMap<>();

    public RedPlayerListCommand(RedPlayerInfo plugin, String name) {
        super(plugin, name);
    }

    public void load() {
        serverBlackList.clear();
        groupBlackList.clear();
        serverAliases.clear();

        serverBlackList.addAll(plugin.getConfig().getStringList("playerlist.server-blacklist"));
        groupBlackList.addAll(plugin.getConfig().getStringList("playerlist.rang-blacklist"));
        for (String serverName : plugin.getConfig().getSection("playerlist.replacements").getKeys()) {
            serverAliases.put(serverName.toLowerCase(), serverName);
            serverAliases.put(plugin.getConfig().getString("playerlist.replacements." + serverName).toLowerCase(), serverName);
        }
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        ServerInfo server = null;
        if (args.length > 0) {
            server = plugin.getProxy().getServerInfo(String.join(" ", args));
            if (server == null) {
                String alias = serverAliases.get(String.join(" ", args).toLowerCase());
                if (alias != null) {
                    server = plugin.getProxy().getServerInfo(alias);
                }
            }
            if (server == null || serverBlackList.contains(server.getName()) || !sender.hasPermission("rwm.playerinfo.playerlist." + server.getName())) {
                sender.sendMessage(BungeePlugin.translate(plugin.getConfig().getString("messages.server-doesnt-exist"), "input", String.join(" ", args)));
                return true;
            }
        } else if (!sender.hasPermission("rwm.playerinfo.playerlist.global")) {
            return false;
        }

        ServerInfo finalServer = server;
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                new PlayerListBuilder(sender, finalServer).build().send();
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Error while building the info string! " + e.getMessage());
            }
        });

        return true;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] args) {
        List<String> playerNames = new ArrayList<>();
        for (ProxiedPlayer player : plugin.getProxy().getPlayers())
            if (args.length == 0 || args.length == 1 && player.getName().startsWith(args[0]))
                playerNames.add(player.getName());
        return playerNames;
    }


    private class PlayerListBuilder {
        private final CommandSender sender;
        private final ServerInfo server;

        private final List<BaseComponent[]> components = new ArrayList<>();

        public PlayerListBuilder(CommandSender sender, ServerInfo server) throws IllegalArgumentException {
            this.sender = sender;
            this.server = server;
        }

        private PlayerListBuilder build() {
            components.add(createComponents("head", getReplacements()));

            Collection<ProxiedPlayer> players = server != null ? server.getPlayers() : plugin.getProxy().getPlayers();
            if (players.isEmpty()) {
                components.add(createComponents("no-player"));
            } else {
                Multimap<RedGroup, RedPlayer> groups = MultimapBuilder.treeKeys().arrayListValues().build();
                for (ProxiedPlayer player : players) {
                    RedPlayer redPlayer = plugin.getPlayer(player);
                    RedGroup redGroup = plugin.getGroup(redPlayer);
                    groups.put(redGroup, redPlayer);
                }

                BaseComponent[] separator = createComponents("player-separator");
                for (Map.Entry<RedGroup, Collection<RedPlayer>> group : groups.asMap().entrySet()) {
                    if (groupBlackList.contains(group.getKey().getName())) {
                        continue;
                    }
                    components.add(createComponents("rang", getReplacements(group.getKey())));
                    List<BaseComponent> playerList = new ArrayList<>();
                    for (RedPlayer redPlayer : group.getValue()) {
                        if (!playerList.isEmpty()) {
                            Collections.addAll(playerList, separator);
                        }
                        Collections.addAll(playerList, createComponents("player", getReplacements(redPlayer)));
                    }
                    components.add(playerList.toArray(new BaseComponent[0]));
                }
            }
            components.add(createComponents("footer", getReplacements()));
            return this;
        }

        private BaseComponent[] createComponents(String key, String... replacements) {
            return MineDown.parse(plugin.getConfig().getString("playerlist.liststyle." + key), replacements);
        }

        private String[] getReplacements() {
            return new String[]{
                    "sendername", sender.getName(),
                    "server", plugin.getConfig().getString("playerlist.replacements." + (server != null ? server.getName() : "global"), server != null ? server.getName() : "global")
            };
        }

        private String[] getReplacements(RedGroup group) {
            return new String[]{
                    "sendername", sender.getName(),
                    "group", group.getDisplayName(),
                    "groupname", group.getName(),
                    "groupprefix", group.getPrefix(),
                    "groupsuffix", group.getSuffix()
            };
        }

        private String[] getReplacements(RedPlayer player) {
            return new String[]{
                    "sendername", sender.getName(),
                    "playername", player.getName(),
                    "playeruuid", player.getUniqueId().toString(),
                    "playerprefix", plugin.getPrefix(player),
                    "playersuffix", plugin.getSuffix(player),
                    "playergroup", plugin.getGroup(player).getDisplayName(),
                    "afk-tag", player.isAfk() ? BungeePlugin.translate(plugin.getConfig().getString("playerlist.liststyle.afk-tag")) : ""
            };
        }

        public void send() {
            for (BaseComponent[] part : components) {
                sender.sendMessage(part);
            }
        }
    }
}
