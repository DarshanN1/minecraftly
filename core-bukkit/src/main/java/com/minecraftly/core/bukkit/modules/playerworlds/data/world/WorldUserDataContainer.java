package com.minecraftly.core.bukkit.modules.playerworlds.data.world;

import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.ContainerUserData;
import org.apache.commons.dbutils.QueryRunner;
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
    protected WorldUserData load(UUID worldUUID) {
        WorldUserData worldUserData = get(worldUUID);

        if (worldUserData == null) { // skip if already loaded
            worldUserData = new WorldUserData(worldUUID, getUser(), getQueryRunnerSupplier());

            try {
                worldUserData.load();
            } catch (SQLException e) {
                e.printStackTrace(); // todo improve
            }

            put(worldUUID, worldUserData);
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
