package com.minecraftly.core.bungee.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;

/**
 * Created by Keir on 17/08/2015.
 * todo in progress
 */
public class SlaveHandler implements Listener {

    public static final String CHANNEL_SERVER_STARTED = "ServerStarted";

    private Gson gson;

    public SlaveHandler(Gson gson) {
        this.gson = gson;
    }

    public void initialize() {

    }

    @EventHandler
    public void onNewServer(PubSubMessageEvent e) {
        if (e.getChannel().equals(CHANNEL_SERVER_STARTED)) {
            JsonObject jsonObject = gson.fromJson(e.getMessage(), JsonObject.class);
            String name = jsonObject.get("name").getAsString();
            InetSocketAddress inetSocketAddress = InetSocketAddress.createUnresolved(jsonObject.get("ip").getAsString(), jsonObject.get("port").getAsInt());

            ServerInfo serverInfo = new BungeeServerInfo(name, inetSocketAddress, false);
            ProxyServer.getInstance().getServers().put(name, serverInfo);
        }
    }

}
