package com.minecraftly.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.bungee.handlers.job.queue.ConnectJobQueue;
import com.minecraftly.PlayerWorldsRepository;
import com.minecraftly.bungee.handlers.job.JobManager;
import com.minecraftly.packets.playerworlds.PacketPlayerGotoWorld;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO fix method names, separate out
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
        Server connectedServer = proxiedPlayer.getServer();
        ServerInfo bestServer = getBestServer(proxiedPlayer, ownerUUID);

        if (bestServer != null && !connectedServer.getInfo().equals(bestServer)) {
            proxiedPlayer.connect(bestServer);
            jobManager.getJobQueue(ConnectJobQueue.class).addJob(proxiedPlayer, (p, s) -> playerGotoWorld(p, ownerUUID));
        } else {
            playerGotoWorld(proxiedPlayer, ownerUUID);
        }
    }

    public ServerInfo getBestServer(ProxiedPlayer proxiedPlayer, UUID ownerUUID) {
        Server server = proxiedPlayer.getServer();
        ServerInfo currentServer = server != null ? server.getInfo() : null;
        ServerInfo hostedServer = getServerHostingWorld(ownerUUID);

        // player needs moved server
        if (hostedServer != null && (currentServer == null || !currentServer.equals(hostedServer))) {
            return hostedServer;
        }

        return currentServer;
    }

    public void playerGotoWorld(ProxiedPlayer proxiedPlayer, UUID worldUUID) {
        String hostingServer = playerWorldsRepository.getServer(worldUUID);

        if (hostingServer != null && !proxiedPlayer.getServer().getInfo().getName().equals(hostingServer)) {
            throw new UnsupportedOperationException("Attempted to host a world on 2 different instances.");
        }

        gateway.sendPacket(proxiedPlayer, new PacketPlayerGotoWorld(proxiedPlayer.getUniqueId(), worldUUID));
    }

    public ServerInfo getServerHostingWorld(UUID worldUUID) {
        String serverName = playerWorldsRepository.getServer(worldUUID);
        return serverName != null ? ProxyServer.getInstance().getServerInfo(serverName) : null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnect(ServerConnectEvent e) {
        ProxiedPlayer proxiedPlayer = e.getPlayer();

        if (proxiedPlayer.getServer() == null) { // initial connect
            try {
                UUID destination = getDestinationWorldID(proxiedPlayer);
                ServerInfo bestServer = getBestServer(proxiedPlayer, destination);

                if (bestServer != null) {
                    e.setTarget(bestServer);
                }

                // otherwise let BungeeCord determine which server to connect to
            } catch (InvalidDestinationException e1) {
                jobManager.getJobQueue(ConnectJobQueue.class).addJob(proxiedPlayer, (p, s) -> {
                    p.sendMessage(new ComponentBuilder("Cannot find player: ")
                            .color(ChatColor.AQUA).append(e1.getInput()).color(ChatColor.GOLD).append(".").create());
                });
            }
        }
    }

    public UUID getDestinationWorldID(ProxiedPlayer proxiedPlayer) throws InvalidDestinationException {
        InetSocketAddress virtualHost = proxiedPlayer.getPendingConnection().getVirtualHost();

        // if connected with a sub-domain, go to the target player
        if (virtualHost != null) {
            String host = virtualHost.getHostString();
            Matcher matcher = DOMAIN_PATTERN.matcher(host);

            if (matcher.find()) {
                String player = matcher.group(1);
                UUID targetUUID = redisBungeeAPI.getUuidFromName(player);

                if (targetUUID != null) {
                    return targetUUID;
                } else {
                    throw new InvalidDestinationException(player); // TODO is this wise?
                }
            }
        }

        return proxiedPlayer.getUniqueId();
    }

    /*@EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPostLogin(PostLoginEvent e) { // go to players world
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

        connectBestServer(proxiedPlayer, destination);
    }*/

}
