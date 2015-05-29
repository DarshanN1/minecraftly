package com.minecraftly.core.bungee;

import com.google.gson.Gson;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

/**
 * Created by Keir on 05/04/2015.
 */
public interface MinecraftlyBungeeCore {

    ProxyServer getProxy();

    ProxyGateway<ProxiedPlayer, ServerInfo> getGateway();

    Configuration getConfiguration();

    RedisBungeeAPI getRedisBungeeAPI();

    Gson getGson();
}
