package com.minecraftly.bukkit.modules.playerworlds.handlers;

import com.minecraftly.bukkit.language.LanguageManager;
import com.minecraftly.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.bukkit.modules.playerworlds.WorldDimension;
import com.minecraftly.bukkit.utilities.BukkitUtilities;
import com.minecraftly.bukkit.language.LanguageValue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.TravelAgent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/**
 * Created by Keir on 11/06/2015.
 */
public class DimensionListener implements Listener {

    private static final Permission PERMISSION_NETHER = new Permission("minecraftly.dimension.nether", PermissionDefault.OP);
    private static final Permission PERMISSION_THE_END = new Permission("minecraftly.dimension.end", PermissionDefault.OP);

    private final ModulePlayerWorlds module;
    private net.milkbowl.vault.permission.Permission permission;
    private final LanguageValue languageNoPermission = new LanguageValue("&cThe owner of this world does not have permission to visit %s.");

    public DimensionListener(ModulePlayerWorlds module, LanguageManager languageManager, net.milkbowl.vault.permission.Permission permission) {
        this.module = module;
        this.permission = permission;
        languageManager.register(module.getLanguageSection() + ".portal.noPermission", languageNoPermission);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPortal(PlayerPortalEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        World fromWorld = from.getWorld();
        World baseWorld = WorldDimension.getBaseWorld(fromWorld);

        if (module.isPlayerWorld(baseWorld)) {
            OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(module.getWorldOwner(baseWorld));
            TravelAgent travelAgent = e.getPortalTravelAgent();

            World.Environment environment = from.getWorld().getEnvironment();
            PlayerTeleportEvent.TeleportCause teleportCause = e.getCause();
            final Location newLocation = player.getLocation().clone();

            if (environment == World.Environment.NORMAL) {
                travelAgent.setSearchRadius(5);

                if (teleportCause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                    if (permission.playerHas(null, offlineOwner, PERMISSION_NETHER.getName())) {
                        WorldDimension.NETHER.convertToLoad(fromWorld, world -> {
                            newLocation.setWorld(world);
                            newLocation.multiply(1D / 8D);
                            BukkitUtilities.asyncLoadAndTeleport(player, travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                        });
                    } else {
                        languageNoPermission.send(player, WorldDimension.NETHER.getNiceName());
                    }
                } else if (teleportCause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                    if (permission.playerHas(null, offlineOwner, PERMISSION_THE_END.getName())) {
                        WorldDimension.THE_END.convertToLoad(fromWorld, world -> {
                            newLocation.setWorld(world);
                            newLocation.multiply(8D);
                            BukkitUtilities.asyncLoadAndTeleport(player, travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                        });
                    } else {
                        languageNoPermission.send(player, WorldDimension.THE_END.getNiceName());
                    }
                }
            } else if (environment == World.Environment.NETHER && teleportCause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                newLocation.setWorld(WorldDimension.getBaseWorld(fromWorld));
                newLocation.multiply(8D);
                travelAgent.setSearchRadius(5);
                BukkitUtilities.asyncLoadAndTeleport(player, travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
            } else if (environment == World.Environment.THE_END && teleportCause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                Location spawnLocation = WorldDimension.getBaseWorld(fromWorld).getSpawnLocation(); // couldn't use newLocation var, it's final
                BukkitUtilities.asyncLoadAndTeleport(player, spawnLocation, PlayerTeleportEvent.TeleportCause.END_PORTAL);
            }
        }
    }

}
