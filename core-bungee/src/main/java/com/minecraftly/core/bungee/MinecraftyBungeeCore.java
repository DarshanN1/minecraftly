package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGateway;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;

/**
 * Created by Keir on 05/04/2015.
 */
public interface MinecraftyBungeeCore {

    ProxyServer getProxy();

    BungeeGateway getGateway();

    Configuration getConfiguration();
}
