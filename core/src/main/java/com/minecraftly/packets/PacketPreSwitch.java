package com.minecraftly.packets;

/**
 * Provides functionality to save player data before they switch server preventing data loss
 */
public enum PacketPreSwitch {

    /**
     * Command for server implementation to save all player data.
     */
    SERVER_SAVE,

    /**
     * Command for proxy implementation to continue the server switch.
     */
    PROXY_SWITCH

}
