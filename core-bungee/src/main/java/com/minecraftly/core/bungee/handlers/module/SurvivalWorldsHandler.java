package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bungee.MinecraftlyBungeeCore;
import com.minecraftly.core.packets.survivalworlds.PacketNoLongerHosting;
import com.minecraftly.core.packets.survivalworlds.PacketPlayerWorld;
import com.sk89q.intake.Command;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 29/04/2015.
 */
public class SurvivalWorldsHandler implements Listener {

    private final MinecraftlyBungeeCore minecraftlyBungeeCore;
    private final Map<ServerInfo, List<UUID>> playerServerMap = new HashMap<>();

    private final String survivalWorldsServerPrefix = "survivalworlds-"; // todo make configurable
    private final int maxWorldsPerServer = 10; // todo make configurable

    public SurvivalWorldsHandler(MinecraftlyBungeeCore minecraftlyBungeeCore) {
        this.minecraftlyBungeeCore = minecraftlyBungeeCore;

        for (Map.Entry<String, ServerInfo> entry : minecraftlyBungeeCore.getProxy().getServers().entrySet()) {
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
            List<UUID> currentServerWorlds = playerServerMap.get(currentServer);

            if (currentServerWorlds != null && currentServerWorlds.size() < maxWorldsPerServer) { // just use current server if possible
                serverInfo = currentServer;
            } else {
                serverInfo = getAvailableServer();
            }
        }

        if (serverInfo != null) {
            sendWorldPacket(serverInfo, proxiedPlayer, ownerUUID);

            if (!proxiedPlayer.getServer().getInfo().equals(serverInfo)) { // only connect if not already connected
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
        } else {
            TextComponent message = new TextComponent("There are currently no free slave servers to host your world."); // todo translatable
            message.setColor(ChatColor.RED);
            proxiedPlayer.sendMessage(message);
        }
    }

    public void sendWorldPacket(ServerInfo serverInfo, ProxiedPlayer proxiedPlayer, UUID ownerUUID) {
        minecraftlyBungeeCore.getGateway().sendPacketServer(serverInfo, new PacketPlayerWorld(proxiedPlayer.getUniqueId(), ownerUUID), true);
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

    // for cases whereby the server disconnecting from has no players online
    // to notify the proxy that it is no longer hosting a world
    // so we'll assume no worlds are being hosted
    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        ServerInfo serverLeaving = e.getTarget();
        if (serverLeaving.getPlayers().size() == 0 && playerServerMap.containsKey(serverLeaving)) {
            playerServerMap.get(serverLeaving).clear();
        }
    }

    @PacketHandler
    public void onNoLongerHosting(ProxiedPlayer proxiedPlayer, PacketNoLongerHosting packet) {
        playerServerMap.get(proxiedPlayer.getServer().getInfo()).remove(packet.getWorldUUID());
    }

}
