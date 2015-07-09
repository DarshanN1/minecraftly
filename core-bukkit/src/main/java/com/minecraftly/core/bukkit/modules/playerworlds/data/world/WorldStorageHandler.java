package com.minecraftly.core.bukkit.modules.playerworlds.data.world;

import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
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

    private ModulePlayerWorlds module;

    public WorldStorageHandler(ModulePlayerWorlds module) {
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
            boolean fromWorld = module.isPlayerWorld(from);
            boolean toWorld = module.isPlayerWorld(to);
            User user = module.getPlugin().getUserManager().getUser(player);
            WorldUserDataContainer worldUserDataContainer = null;

            if (fromWorld) {
                worldUserDataContainer = user.getSingletonUserData(WorldUserDataContainer.class);
                worldUserDataContainer.unload(module.getWorldOwner(from));
            }

            if (toWorld) {
                if (worldUserDataContainer == null) {
                    worldUserDataContainer = user.getSingletonUserData(WorldUserDataContainer.class);
                }

                worldUserDataContainer.load(module.getWorldOwner(to));
            }
        }
    }

}
