package com.minecraftly.core.bukkit.database;

import com.google.common.base.Preconditions;
import org.apache.commons.dbutils.ResultSetHandler;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple implementation for loading yaml configurations from a database via a {@link ResultSet}.
 */
public class YamlConfigurationResultHandler implements ResultSetHandler<YamlConfiguration> {

    public static final YamlConfigurationResultHandler DATA_FIELD_INSTANCE = new YamlConfigurationResultHandler("data");

    private String field;

    public YamlConfigurationResultHandler(String field) {
        this.field = Preconditions.checkNotNull(field);
    }

    @Override
    public YamlConfiguration handle(ResultSet rs) throws SQLException {
        if (rs.next()) {
            try {
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.loadFromString(rs.getString(field));
                return yamlConfiguration;
            } catch (InvalidConfigurationException e) {
                throw new RuntimeException("Unable to parse yml from database.", e); // todo exception type
            }
        }

        return null;
    }
}
