package com.minecraftly.bungee;

import com.google.gson.Gson;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.bungee.handlers.job.JobManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.config.Configuration;

/**
 * Created by Keir on 05/04/2015.
 */
public interface MinecraftlyBungeeCore {

    String getComputeUniqueId();

    ProxyServer getProxy();

    ProxyGateway<ProxiedPlayer, Server, ServerInfo> getGateway();

    Configuration getConfiguration();

    RedisBungeeAPI getRedisBungeeAPI();

    Gson getGson();

    JobManager getJobManager();

}
