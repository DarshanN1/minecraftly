package com.minecraftly.core.bungee.module;

import com.minecraftly.core.bungee.MinecraftyBungeeCore;
import com.minecraftly.core.packets.survivalworlds.PacketPlayerWorld;
import com.sk89q.intake.Command;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 29/04/2015.
 */
public class ModuleSurvivalWorldsHandler {

    private final MinecraftyBungeeCore minecraftyBungeeCore;
    private final Map<ServerInfo, List<UUID>> playerServerMap = new HashMap<>();

    private final String survivalWorldsServerPrefix = "survivalworlds-"; // todo make configurable
    private final int maxWorldsPerServer = 10; // todo make configurable

    public ModuleSurvivalWorldsHandler(MinecraftyBungeeCore minecraftyBungeeCore) {
        this.minecraftyBungeeCore = minecraftyBungeeCore;

        for (Map.Entry<String, ServerInfo> entry : minecraftyBungeeCore.getProxy().getServers().entrySet()) {
            if (entry.getKey().startsWith(survivalWorldsServerPrefix)) {
                playerServerMap.put(entry.getValue(), new ArrayList<UUID>());
            }
        }
    }

    @Command(aliases = "home", desc = "Teleport's the sender to their world")
    public void connectBestServer(ProxiedPlayer proxiedPlayer) {
        connectBestServer(proxiedPlayer, proxiedPlayer.getUniqueId());
    }

    public void connectBestServer(ProxiedPlayer proxiedPlayer, final UUID ownerUUID) {
        ServerInfo serverInfo = getServerHostingWorld(ownerUUID);

        if (serverInfo == null) {
            ServerInfo currentServer = proxiedPlayer.getServer().getInfo();

            if (playerServerMap.containsKey(currentServer) && playerServerMap.get(currentServer).size() < maxWorldsPerServer) { // just use current server if possible
                sendWorldPacket(currentServer, proxiedPlayer, ownerUUID);
            } else {
                serverInfo = getAvailableServer();

                if (serverInfo != null) {
                    sendWorldPacket(serverInfo, proxiedPlayer, ownerUUID);
                    final ServerInfo finalServerInfo = serverInfo;
                    proxiedPlayer.connect(serverInfo, new Callback<Boolean>() {
                        @Override
                        public void done(Boolean success, Throwable throwable) {
                            if (success) {
                                playerServerMap.get(finalServerInfo).add(ownerUUID);
                            }
                        }
                    });
                }
            }
        }
    }

    public void sendWorldPacket(ServerInfo serverInfo, ProxiedPlayer proxiedPlayer, UUID ownerUUID) {
        minecraftyBungeeCore.getGateway().sendPacketServer(serverInfo, new PacketPlayerWorld(proxiedPlayer.getUniqueId(), ownerUUID));
    }

    @Nullable
    public ServerInfo getServerHostingWorld(UUID playerUUID) {
        for (Map.Entry<ServerInfo, List<UUID>> entry : playerServerMap.entrySet()) {
            if (entry.getValue().contains(playerUUID)) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Nullable
    public ServerInfo getAvailableServer() {
        ServerInfo mostFreeServer = null;
        int count = -1;

        for (Map.Entry<ServerInfo, List<UUID>> entry : playerServerMap.entrySet()) {
            int serverCount = entry.getValue().size();

            if (mostFreeServer == null || serverCount < count) {
                mostFreeServer = entry.getKey();
                count = serverCount;
            }
        }

        if (count >= maxWorldsPerServer) {
            mostFreeServer = null;
        }

        return mostFreeServer;
    }

}
