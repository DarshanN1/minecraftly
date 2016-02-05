package com.minecraftly.bukkit.modules.playerworlds.data.world;

import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.user.modularisation.DataStorageHandler;
import com.minecraftly.bukkit.database.DatabaseManager;
import com.minecraftly.bukkit.user.User;
import com.minecraftly.bukkit.user.modularisation.AutoInitializedData;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.sql.SQLException;
import java.util.UUID;

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
                        "`muted` BOOLEAN NOT NULL DEFAULT 0," +
                        "`extra_data` TEXT NOT NULL, PRIMARY KEY (`world_uuid`, `user_uuid`))",
                DatabaseManager.TABLE_PREFIX));
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
            UUID fromWorldOwner = module.getWorldOwner(from);
            UUID toWorldOwner = module.getWorldOwner(to);

            if (fromWorldOwner != null) {
                User user = module.getPlugin().getUserManager().getUser(player);
                user.getSingletonUserData(WorldUserDataContainer.class).unload(fromWorldOwner);
            }

            if (toWorldOwner != null) {
                User ownerUser = module.getPlugin().getUserManager().getUser(toWorldOwner);
                WorldUserDataContainer worldUserDataContainer = ownerUser.getSingletonUserData(WorldUserDataContainer.class);
                worldUserDataContainer.getOrLoad(player.getUniqueId(), true).apply(player);
            }
        }
    }

}
