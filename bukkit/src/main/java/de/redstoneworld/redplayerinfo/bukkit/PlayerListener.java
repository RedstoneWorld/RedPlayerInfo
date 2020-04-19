package de.redstoneworld.redplayerinfo.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class PlayerListener implements Listener {
    private final RedPlayerInfoBukkit plugin;

    public PlayerListener(RedPlayerInfoBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        plugin.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.updateLastActivity((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            plugin.updateLastActivity((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            plugin.updateLastActivity((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            plugin.updateLastActivity((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            plugin.updateLastActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
        ) {
            plugin.updateLastActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onFly(PlayerToggleFlightEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom() != event.getTo()) {
            plugin.updateLastActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        plugin.updateLastActivity(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            plugin.updateLastActivity((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            plugin.updateLastActivity((Player) event.getDamager());
        }
    }
}
