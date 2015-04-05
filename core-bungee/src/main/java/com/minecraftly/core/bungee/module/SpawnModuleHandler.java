package com.minecraftly.core.bungee.module;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bungee.MinecraftyBungeeCore;
import com.minecraftly.core.packets.LocationContainer;
import com.minecraftly.core.packets.PacketTeleport;
import com.minecraftly.core.packets.spawn.PacketSpawn;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Keir on 05/04/2015.
 */
public class SpawnModuleHandler {

    private MinecraftyBungeeCore minecraftly;
    private ServerInfo spawnServer;
    private LocationContainer spawnLocation;

    public SpawnModuleHandler(MinecraftyBungeeCore minecraftly) {
        this.minecraftly = minecraftly;

        Configuration configuration = minecraftly.getConfiguration().getSection("spawn");
        spawnServer = minecraftly.getProxy().getServerInfo(configuration.getString("server"));

        if (spawnServer == null) {
            throw new IllegalArgumentException("Invalid spawn server.");
        }

        spawnLocation = new LocationContainer((Map<String, Object>) configuration.get("location"));
    }

    @PacketHandler
    public void onPacketSpawn(PacketSpawn packetSpawn, final ProxiedPlayer proxiedPlayer) {
        if (proxiedPlayer.getServer().getInfo().equals(spawnServer)) {
            sendTeleportPacket(proxiedPlayer);
        } else {
            proxiedPlayer.connect(spawnServer, new Callback<Boolean>() {
                @Override
                public void done(Boolean success, Throwable throwable) {
                    if (success) {
                        sendTeleportPacket(proxiedPlayer);
                    }
                }
            });
        }
    }

    private void sendTeleportPacket(ProxiedPlayer proxiedPlayer) {
        try {
            minecraftly.getGateway().sendPacket(proxiedPlayer, new PacketTeleport(spawnLocation));
        } catch (IOException e) { // todo
            e.printStackTrace();
        }
    }

}
