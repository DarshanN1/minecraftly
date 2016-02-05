package com.minecraftly.bukkit.modules.playerworlds.data.global;

import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.user.modularisation.DataStorageHandler;
import com.minecraftly.bukkit.database.DatabaseManager;
import com.minecraftly.bukkit.user.User;
import com.minecraftly.bukkit.user.UserManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.sql.SQLException;

/**
 * Created by Keir on 08/06/2015.
 */
public class GlobalStorageHandler extends DataStorageHandler<GlobalUserData> implements Listener {

    private ModulePlayerWorlds module;

    public GlobalStorageHandler(ModulePlayerWorlds module) {
        super(module.getPlugin().getQueryRunnerSupplier());
        this.module = module;
    }

    @Override
    public void initialize() throws SQLException {
        getQueryRunnerSupplier().get().update(
                String.format(
                        "CREATE TABLE IF NOT EXISTS `%sglobal_user_data` (`uuid` BINARY(16) NOT NULL, `extra_data` LONGTEXT NOT NULL, PRIMARY KEY (`uuid`))",
                        DatabaseManager.TABLE_PREFIX
                )
        );
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        World from = WorldDimension.getBaseWorld(e.getFrom().getWorld());
        World to = WorldDimension.getBaseWorld(e.getTo().getWorld());

        if (!from.equals(to)) {
            boolean fromWorld = module.isPlayerWorld(from);
            boolean toWorld = module.isPlayerWorld(to);
            UserManager userManager = module.getPlugin().getUserManager();

            if (fromWorld && !toWorld) {
                User user = userManager.getUser(player, false);

                if (user != null) {
                    user.detachUserData(GlobalUserData.class);
                }
            } else if (!fromWorld && toWorld) {
                User user = userManager.getUser(player);
                GlobalUserData globalUserData = user.getSingletonUserData(GlobalUserData.class);

                if (globalUserData == null) {
                    globalUserData = new GlobalUserData(user, getQueryRunnerSupplier());
                    user.attachUserData(globalUserData);
                    globalUserData.loadAndApply(player);
                } else {
                    try {
                        globalUserData.save();
                    } catch (SQLException e1) {
                        e1.printStackTrace(); // TODO
                    }
                }
            }
        }
    }

}
