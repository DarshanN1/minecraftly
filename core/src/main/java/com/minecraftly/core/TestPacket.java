package com.minecraftly.core;

import com.ikeirnez.pluginmessageframework.packet.Packet;

/**
 * Created by Keir on 03/04/2015.
 */
public class TestPacket extends Packet {

    private static final long serialVersionUID = 8743899430047729993L;

    private final String message;

    public TestPacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
