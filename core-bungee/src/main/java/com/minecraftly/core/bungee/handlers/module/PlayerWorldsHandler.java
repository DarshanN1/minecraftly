package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.core.bungee.HumanCheckManager;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.queue.ConnectJobQueue;
import com.minecraftly.core.bungee.handlers.job.queue.HumanCheckJobQueue;
import com.minecraftly.core.packets.playerworlds.PacketPlayerGotoWorld;
import com.sk89q.intake.Command;
import lc.vq.exhaust.command.annotation.Sender;
import net.md_5.bungee.api.ChatColor;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Keir on 29/04/2015.
 */
public class PlayerWorldsHandler implements Listener {

    private static final Pattern domainPattern = Pattern.compile("^(\\w+)\\.minecraftly\\.org$", Pattern.CASE_INSENSITIVE);

    private final ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private final JobManager jobManager;
    private final HumanCheckManager humanCheckManager;
    private final RedisBungeeAPI redisBungeeAPI;

    // todo use redis
    private final Map<UUID, ServerInfo> worldServerMap = new HashMap<>();

    public PlayerWorldsHandler(ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway,
                               JobManager jobManager,
                               HumanCheckManager humanCheckManager,
                               RedisBungeeAPI redisBungeeAPI) {
        this.gateway = gateway;
        this.jobManager = jobManager;
        this.humanCheckManager = humanCheckManager;
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
            jobManager.getJobQueue(HumanCheckJobQueue.class).addJob(proxiedPlayer, ((proxiedPlayer1, o) -> {
                playerGotoWorld(proxiedPlayer, ownerUUID); // todo duplicate code
            }));
        } else {
            playerGotoWorld(proxiedPlayer, ownerUUID); // todo duplicate code
        }
    }

    public void playerGotoWorld(ProxiedPlayer proxiedPlayer, UUID ownerUUID) {
        playerGotoWorld(proxiedPlayer, ownerUUID, true);
    }

    public void playerGotoWorld(ProxiedPlayer proxiedPlayer, UUID ownerUUID, boolean showNotHumanError) {
        if (showNotHumanError && !humanCheckManager.isHumanVerified(proxiedPlayer)) {
            proxiedPlayer.sendMessage(MclyCoreBungeePlugin.MESSAGE_NOT_HUMAN);
        }

        // this executes immediately if player is already human verified
        jobManager.getJobQueue(HumanCheckJobQueue.class).addJob(proxiedPlayer, (proxiedPlayer1, human) -> {
            if (human) {
                ServerInfo hostingServer = worldServerMap.get(ownerUUID);
                if (hostingServer != null && !proxiedPlayer1.getServer().getInfo().equals(hostingServer)) {
                    throw new UnsupportedOperationException("Attempted to host a world on 2 different instances.");
                }

                gateway.sendPacket(proxiedPlayer1, new PacketPlayerGotoWorld(proxiedPlayer1.getUniqueId(), ownerUUID));
            }
        });
    }

    public ServerInfo getServerHostingWorld(UUID worldUUID) {
        // todo check which server is hosting world remember to check for redis servers too
        return null;
    }

    // for cases whereby the server disconnecting from has no players online
    // to notify the proxy that it is no longer hosting a world
    // so we'll assume no worlds are being hosted
    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        ServerInfo serverLeaving = e.getTarget();
        if (serverLeaving.getPlayers().size() == 0) {
            worldServerMap.values().removeIf(s -> s.equals(serverLeaving));
        }
    }

    /*@PacketHandler todo
    public void onNoLongerHosting(ProxiedPlayer proxiedPlayer, PacketNoLongerHosting packet) {
        playerServerMap.get(proxiedPlayer.getServer().getInfo()).remove(packet.getWorldUUID());
    }*/

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPostLogin(PostLoginEvent e) { // go to players world once they are confirmed to not be a bot
        ProxiedPlayer proxiedPlayer = e.getPlayer();
        UUID destination = proxiedPlayer.getUniqueId();
        InetSocketAddress virtualHost = proxiedPlayer.getPendingConnection().getVirtualHost();

        // if connected with a sub-domain, go to the target player
        if (virtualHost != null) {
            String host = virtualHost.getHostString();
            Matcher matcher = domainPattern.matcher(host);

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

        playerGotoWorld(proxiedPlayer, destination, false);
    }

}
