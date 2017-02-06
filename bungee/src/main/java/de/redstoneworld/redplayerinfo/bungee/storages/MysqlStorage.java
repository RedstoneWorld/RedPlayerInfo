package de.redstoneworld.redplayerinfo.bungee.storages;

import com.zaxxer.hikari.HikariDataSource;
import de.redstoneworld.redplayerinfo.bungee.RedPlayer;
import de.themoep.bungeeplugin.BungeePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class MysqlStorage implements PlayerInfoStorage {
    private final HikariDataSource ds;
    private final BungeePlugin plugin;
    private final String tablePrefix;

    private final CachedStorage cache;

    public MysqlStorage(BungeePlugin plugin) throws SQLException {
        this.plugin = plugin;
        cache = new CachedStorage(plugin);
        plugin.getLogger().info("Loading MysqlStorage");
        this.tablePrefix = plugin.getConfig().getString("mysql.tableprefix");

        String host = plugin.getConfig().getString("mysql.host");
        String port = plugin.getConfig().getString("mysql.port");
        String database = plugin.getConfig().getString("mysql.database");

        ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        ds.setUsername(plugin.getConfig().getString("mysql.user"));
        ds.setPassword(plugin.getConfig().getString("mysql.pass"));
        ds.setConnectionTimeout(5000);

        initializeTables();
    }

    private void initializeTables() throws SQLException {
        try (Connection conn = ds.getConnection(); Statement sta = conn.createStatement()){
            String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players ("
                    + "id INTEGER PRIMARY KEY AUTO_INCREMENT,"
                    + "uuid VARCHAR(36), UNIQUE (uuid), "
                    + "name TINYTEXT,"
                    + "lastlogin BIGINT(11) NOT NULL,"
                    + "lastlogout BIGINT(11) NOT NULL,"
                    + ") DEFAULT CHARACTER SET=utf8 AUTO_INCREMENT=1;";

            sta.execute(sql);
        }
    }

    @Override
    public void savePlayer(RedPlayer player) {
        cache.savePlayer(player);
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            String sql = "INSERT INTO " + tablePrefix + "players (uuid, name, lastlogin, lastlogout) values (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=VALUES(name), lastlogin=VALUES(lastlogin), lastlogout=VALUES(lastlogout);";
            try (Connection conn = ds.getConnection(); PreparedStatement sta = conn.prepareStatement(sql)) {
                sta.setString(1, player.getUniqueId().toString());
                sta.setString(2, player.getName());
                sta.setLong(3, player.getLoginTime());
                sta.setLong(4, player.getLogoutTime());
                sta.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while saving player " + player.getName() + "/" + player.getUniqueId() + " to database!", e);
            }
        });
    }

    @Override
    public RedPlayer getPlayer(String playerName) {
        return cache.getPlayer(playerName, () -> {
            String sql = "SELECT * FROM " + tablePrefix + "players WHERE name=? LIMIT 1";
            try (Connection conn = ds.getConnection(); PreparedStatement sta = conn.prepareStatement(sql)) {
                sta.setString(1, playerName);
                ResultSet rs = sta.executeQuery();
                if (rs.next()) {
                    RedPlayer player = new RedPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    player.setLoginTime(rs.getLong("lastlogin"));
                    player.setLogoutTime(rs.getLong("lastlogout"));
                    return player;
                }
            }
            throw new Exception("No Player with the name " + playerName + " found!");
        });
    }

    @Override
    public RedPlayer getPlayer(UUID playerId) {
        return cache.getPlayer(playerId, () -> {
            String sql = "SELECT * FROM " + tablePrefix + "players WHERE uuid=? LIMIT 1";
            try (Connection conn = ds.getConnection(); PreparedStatement sta = conn.prepareStatement(sql)) {
                sta.setString(1, playerId.toString());
                ResultSet rs = sta.executeQuery();
                if (rs.next()) {
                    RedPlayer player = new RedPlayer(playerId, rs.getString("name"));
                    player.setLoginTime(rs.getLong("lastlogin"));
                    player.setLogoutTime(rs.getLong("lastlogout"));
                    return player;
                }
            }
            throw new Exception("No Player with the UUID " + playerId + " found!");
        });
    }

    @Override
    public void destroy() {
        cache.destroy();
        ds.close();
    }
}
