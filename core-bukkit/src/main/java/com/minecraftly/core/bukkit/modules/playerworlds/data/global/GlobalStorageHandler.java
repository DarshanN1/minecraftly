package com.minecraftly.core.bukkit.modules.playerworlds.data.global;

import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
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
                        "CREATE TABLE IF NOT EXISTS `%sglobal_user_data` (`uuid` BINARY(16) NOT NULL, `data` LONGTEXT NOT NULL, PRIMARY KEY (`uuid`))",
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
                GlobalUserData globalUserData = new GlobalUserData(user, getQueryRunnerSupplier());
                user.attachUserData(globalUserData);
                globalUserData.loadAndApply(player);
            }
        }
    }

}
