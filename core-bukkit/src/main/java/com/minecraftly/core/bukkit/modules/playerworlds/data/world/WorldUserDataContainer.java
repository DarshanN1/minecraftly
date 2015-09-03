package com.minecraftly.core.bukkit.modules.playerworlds.data.world;

import com.minecraftly.core.utilities.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.ContainerUserData;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Created by Keir on 09/06/2015.
 */
public class WorldUserDataContainer extends ContainerUserData<UUID, WorldUserData> {

    public WorldUserDataContainer(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

    @Override
    protected WorldUserData load(UUID userUUID, boolean createIfNotExists) {
        WorldUserData worldUserData = get(userUUID);

        if (worldUserData == null) { // skip if already loaded
            if (!createIfNotExists) {
                try {
                    if (getQueryRunnerSupplier().get().query( // check if user data exists
                            String.format(
                                    "SELECT `world_uuid` FROM `%sworld_user_data` WHERE `world_uuid` = UNHEX(?) AND `user_uuid` = UNHEX(?)",
                                    DatabaseManager.TABLE_PREFIX
                            ), new ArrayHandler(),
                            Utilities.convertToNoDashes(getUser().getUniqueId()),
                            Utilities.convertToNoDashes(userUUID)
                    ).length == 0) {
                        return null;
                    }
                } catch (SQLException e) {
                    e.printStackTrace(); // todo improve
                }
            }

            worldUserData = new WorldUserData(userUUID, getUser(), getQueryRunnerSupplier());

            try {
                worldUserData.load();
            } catch (SQLException e) {
                e.printStackTrace(); // todo improve
            }

            put(userUUID, worldUserData);
        }

        return worldUserData;
    }

    public void unload(UUID worldUUID) {
        WorldUserData worldUserData = get(worldUUID);

        if (worldUserData != null) {
            Player player = getUser().getPlayer();

            if (player != null) {
                try {
                    worldUserData.save();
                } catch (SQLException e) {
                    e.printStackTrace(); // todo make better
                }
            }

            remove(worldUUID);
        }
    }

}
