package com.minecraftly.core.packets.survivalworlds;

import com.ikeirnez.pluginmessageframework.packet.StandardPacket;

import java.util.UUID;

/**
 * Sent from a Slave server to the Proxy, notifying when the server is no longer hosting a world.
 */
public class PacketNoLongerHosting extends StandardPacket {

    private static final long serialVersionUID = 4071317416124081400L;

    private UUID worldUUID;

    public PacketNoLongerHosting(UUID worldUUID) {
        this.worldUUID = worldUUID;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }
}
