package de.redstoneworld.redplayerinfo.bungee.commands;

import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.redstoneworld.redplayerinfo.bungee.RedPlayerInfo;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public class RedPlayerInfoCommand extends PluginCommand implements TabExecutor {

    private final RedPlayerInfo plugin;

    public RedPlayerInfoCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
        this.plugin = (RedPlayerInfo) plugin;
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if (sender.hasPermission("rwm.playerinfo.reload") && "--reload".equalsIgnoreCase(args[0]) || "-r".equals(args[0])) {
            if (plugin.load()) {
                sender.sendMessage(ChatColor.GREEN + "Plugin reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "Error while reloading! Take a look at the console!");
            }
            return true;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            RedPlayer player = null;
            String input = args[0];
            if (input.length() == 32) {
                input = input.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            }

            UUID playerId = null;
            try {
                playerId = UUID.fromString(input);
                player = plugin.getStorage().getPlayer(playerId);
            } catch (IllegalArgumentException e) {
                player = plugin.getStorage().getPlayer(input);
            }

            if (player == null) {
                if (playerId == null) {
                    player = new RedPlayer(new UUID(0, 0), input);
                } else {
                    player = new RedPlayer(playerId, "Name not known.");
                }
                sender.sendMessage(BungeePlugin.translate(plugin.getConfig().getString("messages.unknown-player"), "input", input));
            }

            try {
                new PlayerInfoBuilder(sender, player).build().send();
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


    private class PlayerInfoBuilder {
        private final CommandSender sender;
        private final RedPlayer player;

        private final List<BaseComponent[]> components = new ArrayList<>();
        private final SimpleDateFormat timeFormat;

        public PlayerInfoBuilder(CommandSender sender, RedPlayer player) throws IllegalArgumentException {
            this.sender = sender;
            this.player = player;
            this.timeFormat = new SimpleDateFormat(plugin.getConfig().getString("playerinfo.datetime-format"));
            this.timeFormat.setTimeZone(TimeZone.getTimeZone(plugin.getConfig().getString("playerinfo.datetime-zone")));
        }

        private PlayerInfoBuilder build() {
            components.addAll(createComponents("head"));
            components.addAll(createComponents("info"));
            if (player.isOnline()) {
                components.addAll(createComponents("status.online"));
            } else {
                components.addAll(createComponents("status.offline"));
            }
            if (player.isAfk()) {
                components.addAll(createComponents("status.afk"));
            } else {
                components.addAll(createComponents("status.not-afk"));
            }
            components.addAll(createComponents("extra"));
            return this;
        }

        private List<BaseComponent[]> createComponents(String key) {
            String[] lines = plugin.getConfig().getString("playerinfo." + key).split("\n");
            List<BaseComponent[]> components = new ArrayList<>();
            for (String line : lines) {
                components.add(translate(line));
            }
            return components;
        }

        private BaseComponent[] translate(String message) {
            message = addInfo(message);
            List<String> variables = getVariables(message);
            List<String> parts = new ArrayList<>();
            for (String var : variables) {
                if (plugin.getConfig().isSet("playerinfo.actions." + var)) {
                    int i = message.indexOf("%" + var + "%");
                    parts.add(message.substring(0, i));
                    parts.add("%" + var + "%");
                    message = message.substring(i + ("%" + var + "%").length());
                }
            }
            if (!message.isEmpty()) {
                parts.add(message);
            }

            List<BaseComponent> components = new ArrayList<>();
            for (String part : parts) {
                if (part.startsWith("%") && part.endsWith("%")) {
                    String var = part.substring(1, part.length() - 1);
                    Configuration action = plugin.getConfig().getSection("playerinfo.actions." + var);
                    ComponentBuilder builder = new ComponentBuilder("");
                    if (action != null) {
                        addAction(builder, action);
                    } else {
                        builder.append(part);
                    }
                    components.addAll(Arrays.asList(builder.create()));
                } else {
                    components.addAll(Arrays.asList(TextComponent.fromLegacyText(part)));
                }
            }
            return components.toArray(new BaseComponent[components.size()]);
        }

        private void addAction(ComponentBuilder builder, Configuration action) {
            builder.append(addInfo(action.getString("text")));
            if (action.contains("click-event")) {
                builder.event(new ClickEvent(
                        ClickEvent.Action.valueOf(action.getString("click-event.action").toUpperCase()),
                        addInfo(action.getString("click-event.value"))
                ));
            }
            if (action.contains("hover-event")) {
                builder.event(new HoverEvent(
                        HoverEvent.Action.valueOf(action.getString("hover-event.action").toUpperCase()),
                        TextComponent.fromLegacyText(addInfo(action.getString("hover-event.value")))
                ));
            }
        }

        private String addInfo(String message) {
            return BungeePlugin.translate(message,
                    "sendername", sender.getName(),
                    "playername", player.getName(),
                    "playeruuid", player.getUniqueId().toString(),
                    "playerprefix", plugin.getPrefix(player),
                    "playersuffix", plugin.getSuffix(player),
                    "playergroup", plugin.getGroup(player),
                    "logintime", formatTime(player.getLoginTime()),
                    "logouttime", formatTime(player.getLogoutTime()),
                    "afktime", formatTime(player.getAfkTime()),
                    "afkreason", player.getAfkMessage()
            );
        }

        private String formatTime(long timeStamp) {
            return timeFormat.format(new Date(timeStamp));
        }

        private List<String> getVariables(String message) {
            List<String> variables = new ArrayList<>();
            StringBuilder currentVar = null;
            boolean escaped = false;
            for (char c : message.toCharArray()) {
                if (!escaped && c == '%') {
                    if (currentVar == null) {
                         currentVar = new StringBuilder();
                    } else {
                        variables.add(currentVar.toString());
                        currentVar = null;
                    }
                } else if (c == '\\') {
                    escaped = true;
                } else if (c != ' ' && currentVar != null) {
                    currentVar.append(c);
                    escaped = false;
                }
            }
            return variables;
        }

        public void send() {
            for (BaseComponent[] part : components) {
                sender.sendMessage(part);
            }
        }
    }
}
