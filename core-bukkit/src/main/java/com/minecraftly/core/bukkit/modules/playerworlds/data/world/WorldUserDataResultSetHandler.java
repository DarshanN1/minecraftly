package com.minecraftly.core.bukkit.modules.playerworlds.data.world;

import com.minecraftly.core.bukkit.database.YamlConfigurationResultHandler;
import org.apache.commons.dbutils.ResultSetHandler;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An extension of {@link WorldUserDataResultSetHandler} handling other bits of data such as player mute status'.
 */
public class WorldUserDataResultSetHandler implements ResultSetHandler<YamlConfiguration> {

    public static final WorldUserDataResultSetHandler INSTANCE = new WorldUserDataResultSetHandler();

    @Override
    public YamlConfiguration handle(ResultSet rs) throws SQLException {
        YamlConfiguration yamlConfiguration = YamlConfigurationResultHandler.EXTRA_DATA_FIELD_HANDLER_INSTANCE.handle(rs);

        if (yamlConfiguration != null) {
            yamlConfiguration.set("muted", rs.getBoolean("muted"));
        }

        return yamlConfiguration;
    }

}
