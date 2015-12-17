package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.queue.ConnectJobQueue;
import com.minecraftly.core.packets.playerworlds.PacketNoLongerHostingWorld;
import com.minecraftly.core.packets.playerworlds.PacketPlayerGotoWorld;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Keir on 29/04/2015.
 */
public class PlayerWorldsHandler implements Listener {

    // ^<username (min 1, max 16)>.<domain (min 1, max inf)>.<extension (min 2, max 4)>
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^(\\w{1,16})\\.(\\w+)\\.(\\w{2,4})$", Pattern.CASE_INSENSITIVE);

    private final ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private final JobManager jobManager;
    private final PlayerWorldsRepository playerWorldsRepository;
    private final RedisBungeeAPI redisBungeeAPI;

    public PlayerWorldsHandler(ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway,
                               JobManager jobManager,
                               PlayerWorldsRepository playerWorldsRepository,
                               RedisBungeeAPI redisBungeeAPI) {
        this.gateway = gateway;
        this.jobManager = jobManager;
        this.playerWorldsRepository = playerWorldsRepository;
        this.redisBungeeAPI = redisBungeeAPI;
    }

    @Command(aliases = "home", desc = "Teleport's the sender to their world")
    public void connectBestServer(@Sender ProxiedPlayer proxiedPlayer) {
        connectBestServer(proxiedPlayer, proxiedPlayer.getUniqueId());
    }

    public void connectBestServer(ProxiedPlayer proxiedPlayer, final UUID ownerUUID) {
        ServerInfo serverInfo = getServerHostingWorld(ownerUUID);
        if (serverInfo != null && !proxiedPlayer.getServer().getInfo().equals(serverInfo)) { // connect to server this should be hosted on
            proxiedPlayer.connect(serverInfo);
            playerGotoWorld(proxiedPlayer, ownerUUID);
        } else {
            playerGotoWorld(proxiedPlayer, ownerUUID);
        }
    }

    public void playerGotoWorld(ProxiedPlayer proxiedPlayer, UUID ownerUUID) {
        String hostingServer = playerWorldsRepository.getServer(ownerUUID);

        if (hostingServer != null && !proxiedPlayer.getServer().getInfo().getName().equals(hostingServer)) {
            throw new UnsupportedOperationException("Attempted to host a world on 2 different instances.");
        }

        gateway.sendPacket(proxiedPlayer, new PacketPlayerGotoWorld(proxiedPlayer.getUniqueId(), ownerUUID));
    }

    public ServerInfo getServerHostingWorld(UUID worldUUID) {
        String serverName = playerWorldsRepository.getServer(worldUUID);
        return serverName != null ? ProxyServer.getInstance().getServerInfo(serverName) : null;
    }

    // for cases whereby the server disconnecting from has no players online
    // to notify the proxy that it is no longer hosting a world
    // so we'll assume no worlds are being hosted
    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        ServerInfo serverLeaving = e.getTarget();
        if (serverLeaving.getPlayers().size() == 0) {
            playerWorldsRepository.removeAll(serverLeaving.getName());
        }
    }

    @PacketHandler
    public void onNoLongerHosting(ProxiedPlayer proxiedPlayer, PacketNoLongerHostingWorld packet) {
        playerWorldsRepository.setServer(packet.getWorldUUID(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPostLogin(PostLoginEvent e) { // go to players world once they are confirmed to not be a bot
        ProxiedPlayer proxiedPlayer = e.getPlayer();
        UUID destination = proxiedPlayer.getUniqueId();
        InetSocketAddress virtualHost = proxiedPlayer.getPendingConnection().getVirtualHost();

        // if connected with a sub-domain, go to the target player
        if (virtualHost != null) {
            String host = virtualHost.getHostString();
            Matcher matcher = DOMAIN_PATTERN.matcher(host);

            if (matcher.find()) {
                String player = matcher.group(1);
                UUID targetUUID = redisBungeeAPI.getUuidFromName(player);

                if (targetUUID != null) {
                    destination = targetUUID;
                } else {
                    jobManager.getJobQueue(ConnectJobQueue.class).addJob(proxiedPlayer, (p, s) -> {
                        p.sendMessage(new ComponentBuilder("Cannot find player: ").color(ChatColor.AQUA).append(player).color(ChatColor.GOLD).append(".").create());
                    });
                }
            }
        }

        playerGotoWorld(proxiedPlayer, destination);
    }

}
