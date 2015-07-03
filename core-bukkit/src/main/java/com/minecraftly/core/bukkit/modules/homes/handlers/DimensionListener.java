package com.minecraftly.core.bukkit.modules.homes.handlers;

import com.minecraftly.core.bukkit.modules.homes.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.homes.WorldDimension;
import org.bukkit.Location;
import org.bukkit.TravelAgent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Created by Keir on 11/06/2015.
 */
public class DimensionListener implements Listener {

    private ModulePlayerWorlds module;

    public DimensionListener(ModulePlayerWorlds module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPortal(PlayerPortalEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        World fromWorld = from.getWorld();

        if (module.isHomeWorld(WorldDimension.getBaseWorld(fromWorld))) {
            TravelAgent travelAgent = e.getPortalTravelAgent();

            World.Environment environment = from.getWorld().getEnvironment();
            PlayerTeleportEvent.TeleportCause teleportCause = e.getCause();
            Location newLocation = player.getLocation().clone();

            if (environment == World.Environment.NORMAL) {
                travelAgent.setSearchRadius(5);

                if (teleportCause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                    newLocation.setWorld(WorldDimension.NETHER.convertTo(fromWorld, true));
                    newLocation.multiply(1D / 8D);
                    player.teleport(travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                } else if (teleportCause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                    newLocation.setWorld(WorldDimension.THE_END.convertTo(fromWorld, true));
                    newLocation.multiply(8D);
                    player.teleport(travelAgent.findOrCreate(newLocation), PlayerTeleportEvent.TeleportCause.END_PORTAL);
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
