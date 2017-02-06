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
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedPlayerInfoCommand extends PluginCommand {

    private final Pattern varPattern = Pattern.compile("%(\\w+)%");

    public RedPlayerInfoCommand(BungeePlugin plugin, String name, String permission, String permissionMessage, String description, String usage, String... aliases) {
        super(plugin, name, permission, permissionMessage, description, usage, aliases);
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if ("--reload".equalsIgnoreCase(args[0]) || "-r".equals(args[0])) {
            ((RedPlayerInfo) plugin).load();
            sender.sendMessage(ChatColor.YELLOW + "Plugin reloaded.");
            return true;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            RedPlayer player = null;
            String input = args[0];
            if (input.length() == 32) {
                input = input.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            }

            try {
                player = ((RedPlayerInfo) plugin).getStorage().getPlayer(UUID.fromString(input));
            } catch (IllegalArgumentException e) {
                player = ((RedPlayerInfo) plugin).getStorage().getPlayer(input);
            }

            if (player == null) {
                sender.sendMessage(plugin.translate(plugin.getConfig().getString("messages.unknown-player"), "input", input));
                return;
            }

            new PlayerInfoBuilder(sender, player).build().send();
        });

        return true;
    }


    private class PlayerInfoBuilder {
        private final CommandSender sender;
        private final RedPlayer player;

        private final List<BaseComponent[]> components = new ArrayList<>();

        public PlayerInfoBuilder(CommandSender sender, RedPlayer player) {
            this.sender = sender;
            this.player = player;
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
            String[] lines = plugin.getConfig().getString("playerinfo." + key).split("\\n");
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
            return plugin.translate(message,
                    "sendername", sender.getName(),
                    "playername", player.getName(),
                    "playeruuid", player.getUniqueId().toString()
            );
        }

        private List<String> getVariables(String message) {
            Matcher matcher = varPattern.matcher(message);
            List<String> variables = new ArrayList<>();
            if (matcher.matches()) {
                for (int i = 1; i < matcher.groupCount(); i++) {
                    variables.add(matcher.group(i));
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
