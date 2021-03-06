package de.redstoneworld.redplayerinfo.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class RedPlayer {

    private final UUID uniqueId;
    private final String name;
    private long loginTime = -1;
    private long logoutTime = -1;
    private long afkTime = -1;
    private String afkMessage;
    private boolean afkManually = false;

    public RedPlayer(ProxiedPlayer player) {
        this(player.getUniqueId(), player.getName());
    }

    public RedPlayer(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }

    public long getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(long logoutTime) {
        this.logoutTime = logoutTime;
    }

    public long getAfkTime() {
        return afkTime;
    }

    public String getAfkMessage() {
        return isAfk() ? afkMessage : "";
    }

    public void setAfk(String afkMessage, boolean manual) {
        afkTime = System.currentTimeMillis();
        this.afkMessage = afkMessage;
        this.afkManually = manual;
    }

    public void unsetAfk() {
        afkTime = -1;
        this.afkMessage = null;
    }

    public boolean isAfk() {
        return afkMessage != null;
    }

    public boolean isOnline() {
        return logoutTime < 0 && loginTime > 0 && loginTime <= System.currentTimeMillis();
    }

    public boolean isAutoAfk() {
        return isAfk() && !afkManually;
    }
}
