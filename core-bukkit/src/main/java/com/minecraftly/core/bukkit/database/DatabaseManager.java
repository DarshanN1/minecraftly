package com.minecraftly.core.bukkit.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Keir on 15/03/2015.
 */
public class DatabaseManager {

    final String username;
    private final Logger logger;
    private final String prefix;
    private final String host;
    private final String password;
    private final String database;
    private final int port;

    private HikariDataSource hikariDataSource;

    public DatabaseManager(Logger logger, Map<String, Object> data) {
        this(logger,
                (String) data.get("host"),
                (String) data.get("username"),
                (String) data.get("password"),
                (String) data.get("database"),
                (int) data.get("port"),
                (String) data.get("prefix"));
    }

    public DatabaseManager(Logger logger, String host, String username, String password, String database, int port, String prefix) {
        checkNotNull(logger);
        checkNotNull(host);
        checkNotNull(username);
        checkNotNull(password);
        checkNotNull(database);

        if (prefix == null) {
            prefix = "";
        }

        this.logger = logger;
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;
        this.prefix = prefix;
    }

    public void connect() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setInitializationFailFast(true);
        hikariConfig.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(10));
        hikariConfig.setMaximumPoolSize(25); // todo adjust
        hikariConfig.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikariConfig.addDataSourceProperty("serverName", host);
        hikariConfig.addDataSourceProperty("port", port);
        hikariConfig.addDataSourceProperty("databaseName", database);
        hikariConfig.addDataSourceProperty("user", username);
        hikariConfig.addDataSourceProperty("password", password);

        logger.info("Connecting to host '" + getHost() + "'");
        hikariDataSource = new HikariDataSource(hikariConfig);
    }

    public void disconnect() {
        hikariDataSource.close();
    }

    public HikariDataSource getHikariDataSource() {
        return hikariDataSource;
    }

    public Connection getConnection() {
        try {
            return getHikariDataSource().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error obtaining database connection.", e);
        }

        return null;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getDatabase() {
        return database;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public String getPrefix() {
        return prefix;
    }
}
