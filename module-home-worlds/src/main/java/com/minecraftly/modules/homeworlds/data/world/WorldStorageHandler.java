package com.minecraftly.modules.homeworlds.data.world;

import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
import com.minecraftly.modules.homeworlds.HomeWorldsModule;
import com.minecraftly.modules.homeworlds.WorldDimension;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.sql.SQLException;

/**
 * Created by Keir on 09/06/2015.
 */
public class WorldStorageHandler extends DataStorageHandler<WorldUserDataContainer> implements Listener, AutoInitializedData<WorldUserDataContainer> {

    private HomeWorldsModule module;

    public WorldStorageHandler(HomeWorldsModule module) {
        super(module.getPlugin().getQueryRunnerSupplier());
        this.module = module;
    }

    @Override
    public void initialize() throws SQLException {
        getQueryRunnerSupplier().get().update(String.format(
                "CREATE TABLE IF NOT EXISTS `%sworld_user_data` (" +
                        "`world_uuid` BINARY(16) NOT NULL, " +
                        "`user_uuid` BINARY(16) NOT NULL," +
                        "`data` TEXT NOT NULL, PRIMARY KEY (`world_uuid`, `user_uuid`))", DatabaseManager.TABLE_PREFIX));
    }

    @Override
    public WorldUserDataContainer autoInitialize(User user) {
        return new WorldUserDataContainer(user, getQueryRunnerSupplier());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        World from = WorldDimension.getBaseWorld(e.getFrom().getWorld());
        World to = WorldDimension.getBaseWorld(e.getTo().getWorld());

        if (!from.equals(to)) {
            boolean fromHome = module.isHomeWorld(from);
            boolean toHome = module.isHomeWorld(to);
            User user = module.getPlugin().getUserManager().getUser(player);
            WorldUserDataContainer worldUserDataContainer = null;

            if (fromHome) {
                worldUserDataContainer = user.getSingletonUserData(WorldUserDataContainer.class);
                worldUserDataContainer.unload(module.getHomeOwner(from));
            }

            if (toHome) {
                if (worldUserDataContainer == null) {
                    worldUserDataContainer = user.getSingletonUserData(WorldUserDataContainer.class);
                }

                worldUserDataContainer.load(module.getHomeOwner(to));
            }
        }
    }

}
