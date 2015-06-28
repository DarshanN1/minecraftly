package com.minecraftly.core.packets;

import com.ikeirnez.pluginmessageframework.packet.StandardPacket;

/**
 * Created by Keir on 27/06/2015.
 */
public class PacketBotCheck extends StandardPacket {

    private static final long serialVersionUID = -8760082548212552342L;

    // 0 = ask
    // 1 = response
    private final int stage;
    private boolean response;

    public PacketBotCheck() {
        this.stage = 0;
    }

    public PacketBotCheck(boolean response) {
        this.stage = 1;
        this.response = response;
    }

    public int getStage() {
        return stage;
    }

    public boolean getResponse() {
        return response;
    }

}
