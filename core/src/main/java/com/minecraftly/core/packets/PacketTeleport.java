package com.minecraftly.core.packets;

import com.google.common.base.Preconditions;
import com.ikeirnez.pluginmessageframework.packet.Packet;

/**
 * Created by Keir on 05/04/2015.
 */
public class PacketTeleport extends Packet {

    private static final long serialVersionUID = 4714156896979723677L;

    private LocationContainer locationContainer;

    public PacketTeleport(LocationContainer locationContainer) {
        this.locationContainer = Preconditions.checkNotNull(locationContainer);
    }

    public LocationContainer getLocationContainer() {
        return locationContainer;
    }
}
