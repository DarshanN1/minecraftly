package com.minecraftly.core.packets.playerworlds;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.packet.StandardPacket;

import java.util.UUID;

/**
 * Sent to a survival world slave, loads a world (if needed) and queues teleport task for when the player joins.
 */
public class PacketPlayerGotoWorld extends StandardPacket {

    private static final long serialVersionUID = 8425511690002980703L;

    private final UUID player;
    private final UUID world;

    public PacketPlayerGotoWorld(UUID player) {
        this(player, player);
    }

    public PacketPlayerGotoWorld(UUID player, UUID world) {
        this.player = Preconditions.checkNotNull(player);
        this.world = Preconditions.checkNotNull(world);
    }

    public UUID getPlayer() {
        return player;
    }

    public UUID getWorld() {
        return world;
    }
}
