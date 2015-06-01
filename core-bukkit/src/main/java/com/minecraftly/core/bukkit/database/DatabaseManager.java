package com.minecraftly.core.bukkit.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private HikariDataSource dataSource;
    private QueryRunner queryRunner;

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

        logger.info("Connecting to database on host '" + host + "'.");
        dataSource = new HikariDataSource(hikariConfig);
        queryRunner = new QueryRunner(dataSource);
    }

    public void disconnect() {
        queryRunner = null;
        dataSource.close();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public QueryRunner getQueryRunner() {
        return queryRunner;
    }

    public Connection getConnection() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error obtaining database connection.", e);
        }

        return null;
    }

}
