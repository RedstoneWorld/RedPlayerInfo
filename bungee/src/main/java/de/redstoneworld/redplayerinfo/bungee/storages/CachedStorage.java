package de.redstoneworld.redplayerinfo.bungee.storages;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.themoep.bungeeplugin.BungeePlugin;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CachedStorage implements PlayerInfoStorage {

    private Cache<UUID, RedPlayer> idCache;

    private Cache<String, RedPlayer> nameCache;

    public CachedStorage(BungeePlugin plugin) {
        idCache = CacheBuilder.newBuilder()
                .maximumSize(plugin.getConfig().getInt("cache-size"))
                .build();
        nameCache = CacheBuilder.newBuilder()
                .maximumSize(plugin.getConfig().getInt("cache-size"))
                .build();
    }

    public void savePlayer(RedPlayer player) {
        idCache.put(player.getUniqueId(), player);
        nameCache.put(player.getName().toLowerCase(Locale.ROOT), player);
    }

    public RedPlayer getPlayer(String playerName) {
        return nameCache.getIfPresent(playerName.toLowerCase(Locale.ROOT));
    }

    public RedPlayer getPlayer(UUID playerId) {
        return idCache.getIfPresent(playerId);
    }

    @Override
    public void destroy() {
        idCache = null;
        nameCache = null;
    }

    @Override
    public Collection<RedPlayer> getCachedPlayers() {
        return idCache.asMap().values();
    }

    public RedPlayer getPlayer(String playerName, Callable<RedPlayer> nameCacheLoader) {
        try {
            return nameCache.get(playerName.toLowerCase(Locale.ROOT), nameCacheLoader);
        } catch (ExecutionException e) {
            return null;
        }
    }

    public RedPlayer getPlayer(UUID playerId, Callable<RedPlayer> idCacheLoader) {
        try {
            return idCache.get(playerId, idCacheLoader);
        } catch (ExecutionException e) {
            return null;
        }
    }
}
