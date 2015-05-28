package com.minecraftly.core.packets;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.packet.Packet;

import java.util.UUID;

/**
 * Packet used for instructing a server instance to teleport a player to another location.
 */
public class PacketTeleport extends Packet {

    private static final long serialVersionUID = 4714156896979723677L;

    private UUID playerUUID = null;
    private LocationContainer locationContainer = null;

    public PacketTeleport(UUID playerUUID) {
        this.playerUUID = Preconditions.checkNotNull(playerUUID);
    }

    public PacketTeleport(LocationContainer locationContainer) {
        this.locationContainer = Preconditions.checkNotNull(locationContainer);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public LocationContainer getLocationContainer() {
        return locationContainer;
    }
}
