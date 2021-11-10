package me.albert.skullapi.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.albert.skullapi.SkullAPI;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

;

public class MySQL {

    public static boolean ENABLED = false;

    private static HikariDataSource dataSource;
    private static String DATABASE;
    private static String TABLE = "skulls";

    public static void setUP() {
        ENABLED = true;
        FileConfiguration cfg = SkullAPI.mysqlSettings.getConfig();
        HikariConfig config = new HikariConfig();
        config.setPoolName(SkullAPI.getInstance().getName());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(cfg.getString("storage.username"));
        config.setPassword(cfg.getString("storage.password"));
        Properties properties = new Properties();
        String jdbcUrl = "jdbc:mysql://" + cfg.getString("storage.host") + ':' +
                cfg.getString("storage.port") + '/' + cfg.getString("storage.database");
        DATABASE = cfg.getString("storage.database");
        TABLE = cfg.getString("storage.table");
        properties.setProperty("useSSL", cfg.getString("storage.useSSL"));
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);
        properties.setProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");
        config.setJdbcUrl(jdbcUrl);
        config.setDataSourceProperties(properties);
        dataSource = new HikariDataSource(config);
        try {
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (!hasData()) {
            SkullAPI.getInstance().getLogger().info("§cNo data in mysql,import from yaml...");
            FileConfiguration data = SkullAPI.getInstance().getConfig();
            int imported = 0;
            for (String player : data.getConfigurationSection("").getKeys(false)) {
                String signature = data.getString(player + ".signature");
                String value = data.getString(player + ".value");
                imported++;
                MySQL.savePlayer(player, new String[]{signature, value});
            }
            SkullAPI.getInstance().getLogger().info("§cData imported: " + imported);
        }
    }

    public static void createTables() throws SQLException {
        String file = "/create.sql";
        try (InputStream in = SkullAPI.getInstance().getClass().getResourceAsStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                builder.append(line);
                if (line.endsWith(";")) {
                    String sql = builder.toString();
                    stmt.addBatch(String.format(sql, TABLE));
                    builder = new StringBuilder();
                }
            }
            stmt.executeBatch();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static boolean hasData() {
        String sql = "SELECT * FROM `%s`.`%s` LIMIT 1;";
        sql = String.format(sql, DATABASE, TABLE);
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        return false;
    }

    public static void savePlayer(String player, String[] skin) {
        if (getPlayer(player) != null) {
            String sql = "UPDATE `%s`.`%s` SET `signature`=?, `value`=?  WHERE  `player`=?;";
            sql = String.format(sql, DATABASE, TABLE);
            try (Connection con = dataSource.getConnection();
                 PreparedStatement stmt = con.prepareStatement(sql, RETURN_GENERATED_KEYS)) {
                stmt.setString(1, skin[0]);
                stmt.setString(2, skin[1]);
                stmt.setString(3, player);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        String sql = "INSERT INTO `%s`.`%s` (`player`, `signature`, `value`) VALUES(?,?,?) ON DUPLICATE KEY UPDATE signature=?,value=?;";
        sql = String.format(sql, DATABASE, TABLE);
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql, RETURN_GENERATED_KEYS)) {
            stmt.setString(1, player);
            stmt.setString(2, skin[0]);
            stmt.setString(3, skin[1]);
            stmt.setString(4, skin[0]);
            stmt.setString(5, skin[1]);
            stmt.executeUpdate();
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
    }

    public static String[] getPlayer(String name) {
        String sql = "SELECT `id`, `player`, `signature` , `value` FROM `%s`.`%s` WHERE `player`=?;";
        sql = String.format(sql, DATABASE, TABLE);
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    String signature = resultSet.getString("signature");
                    String value = resultSet.getString("value");
                    return new String[]{signature, value};
                }
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        return null;
    }


    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }


}
