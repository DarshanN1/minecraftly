package com.minecraftly.core.bungee.handlers.job.handlers;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bungee.HumanCheckManager;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.queue.HumanCheckJobQueue;
import com.minecraftly.core.packets.PacketBotCheck;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created by Keir on 28/06/2015.
 */
public class HumanCheckHandler implements Listener {

    private final JobManager jobManager;
    private final HumanCheckManager humanCheckManager;
    private final ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;

    public HumanCheckHandler(HumanCheckManager humanCheckManager, JobManager jobManager, ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway) {
        this.humanCheckManager = humanCheckManager;
        this.jobManager = jobManager;
        this.gateway = gateway;
    }

    @EventHandler
    public void onServerConnection(ServerConnectedEvent e) {
        ProxiedPlayer proxiedPlayer = e.getPlayer();

        if (!humanCheckManager.isHumanVerified(proxiedPlayer)) {
            gateway.sendPacketServer(e.getServer(), new PacketBotCheck());
        }
    }

    @SuppressWarnings("unchecked")
    @PacketHandler
    public void onPacketBotCheck(PacketBotCheck packetBotCheck, ProxiedPlayer proxiedPlayer) {
        if (packetBotCheck.getStage() != 1) {
            throw new UnsupportedOperationException("Expecting response 1.");
        }

        boolean response = packetBotCheck.getResponse();

        if (response) {
            humanCheckManager.addHumanVerified(proxiedPlayer.getUniqueId());
        }

        jobManager.getJobQueue(HumanCheckJobQueue.class).executeJobs(proxiedPlayer, packetBotCheck.getResponse());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        humanCheckManager.removeHumanVerified(e.getPlayer());
    }

}
