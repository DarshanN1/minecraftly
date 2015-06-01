package com.minecraftly.core.bukkit.listeners;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.LocationContainer;
import com.minecraftly.core.packets.PacketTeleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/**
 * Handles any core incoming packets.
 */
public class PacketListener {

    @PacketHandler
    public void onPacketTeleport(Player player, PacketTeleport packetTeleport) {
        Location location = null;
        UUID playerUUID = packetTeleport.getPlayerUUID();
        LocationContainer locationContainer = packetTeleport.getLocationContainer();

        if (playerUUID != null) {
            Player target = Bukkit.getPlayer(playerUUID);
            if (target != null) {
                location = target.getLocation();
            }
        } else if (locationContainer != null) {
            location = BukkitUtilities.getLocation(locationContainer);

            if (location.getWorld() == null) {
                throw new IllegalArgumentException("Invalid spawn location, world '" + locationContainer.getWorld() + "' doesn't exist.");
            }
        } else {
            throw new UnsupportedOperationException("Don't know how to handle a teleport packet with all null parameters.");
        }

        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

}
