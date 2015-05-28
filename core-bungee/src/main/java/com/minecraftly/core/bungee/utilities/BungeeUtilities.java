package com.minecraftly.core.bungee.utilities;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;

/**
 * A collection of utility methods to aid in the writing of BungeeCord plugins.
 */
public class BungeeUtilities {

    private BungeeUtilities() {}

    public static ProxiedPlayer easyMatchPlayer(String name) {
        Collection<ProxiedPlayer> results = ProxyServer.getInstance().matchPlayer(name);
        return results.size() > 0 ? results.iterator().next() : null;
    }

}
