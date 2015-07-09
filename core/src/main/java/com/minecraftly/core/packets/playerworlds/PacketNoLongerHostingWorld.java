package com.minecraftly.core.packets.playerworlds;

import com.ikeirnez.pluginmessageframework.packet.StandardPacket;

import java.util.UUID;

/**
 * Sent from a Slave server to the Proxy, notifying when the server is no longer hosting a world.
 */
public class PacketNoLongerHostingWorld extends StandardPacket {

    private static final long serialVersionUID = 4071317416124081400L;

    private UUID worldUUID;

    public PacketNoLongerHostingWorld(UUID worldUUID) {
        this.worldUUID = worldUUID;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }
}
