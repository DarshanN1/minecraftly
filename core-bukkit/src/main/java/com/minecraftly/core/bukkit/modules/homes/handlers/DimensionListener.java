package com.minecraftly.core.bukkit.modules.homes.handlers;

import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.modules.homes.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.homes.WorldDimension;
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

        if (module.isHomeWorld(baseWorld)) {
            OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(module.getHomeOwner(baseWorld));
            TravelAgent travelAgent = e.getPortalTravelAgent();

            World.Environment environment = from.getWorld().getEnvironment();
            PlayerTeleportEvent.TeleportCause teleportCause = e.getCause();
            Location newLocation = player.getLocation().clone();

            if (environment == World.Environment.NORMAL) {
                travelAgent.setSearchRadius(5);

                if (teleportCause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                    if (permission.playerHas(null, offlineOwner, PERMISSION_NETHER.getName())) {
                        newLocation.setWorld(WorldDimension.NETHER.convertTo(fromWorld, true));
                        newLocation.multiply(1D / 8D);
                        player.teleport(travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                    } else {
                        languageNoPermission.send(player, WorldDimension.NETHER.getNiceName());
                    }
                } else if (teleportCause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                    if (permission.playerHas(null, offlineOwner, PERMISSION_THE_END.getName())) {
                        newLocation.setWorld(WorldDimension.THE_END.convertTo(fromWorld, true));
                        newLocation.multiply(8D);
                        player.teleport(travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                    } else {
                        languageNoPermission.send(player, WorldDimension.THE_END.getNiceName());
                    }
                }
            } else if (environment == World.Environment.NETHER && teleportCause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                newLocation.setWorld(WorldDimension.getBaseWorld(fromWorld));
                newLocation.multiply(8D);
                travelAgent.setSearchRadius(5);
                player.teleport(travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
            } else if (environment == World.Environment.THE_END && teleportCause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                newLocation = WorldDimension.getBaseWorld(fromWorld).getSpawnLocation();
                player.teleport(newLocation, PlayerTeleportEvent.TeleportCause.END_PORTAL);
            }
        }
    }

}
