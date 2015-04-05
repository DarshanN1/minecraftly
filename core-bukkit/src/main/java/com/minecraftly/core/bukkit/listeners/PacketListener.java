package com.minecraftly.core.bukkit.listeners;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.packets.PacketTeleport;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Created by Keir on 05/04/2015.
 */
public class PacketListener {

    @PacketHandler
    public void onPacketTeleport(Player player, PacketTeleport packetTeleport) {
        player.teleport(BukkitUtilities.getLocation(packetTeleport.getLocationContainer()), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

}
