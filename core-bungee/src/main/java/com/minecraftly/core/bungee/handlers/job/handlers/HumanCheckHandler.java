package com.minecraftly.core.bungee.handlers.job.handlers;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.JobType;
import com.minecraftly.core.bungee.handlers.job.JobQueue;
import com.minecraftly.core.packets.PacketBotCheck;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Created by Keir on 28/06/2015.
 */
public class HumanCheckHandler extends JobQueue<Boolean> implements Listener {

    private final JobManager jobManager;
    private final List<UUID> humanVerified = new ArrayList<>();

    public HumanCheckHandler(JobManager jobManager) {
        super(Boolean.class);
        this.jobManager = jobManager;
    }

    @Override
    public void addJob(UUID playerUUID, BiConsumer<ProxiedPlayer, Boolean> consumer) {
        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerUUID);

        if (isHumanVerified(playerUUID) && proxiedPlayer != null) { // short-circuit
            consumer.accept(proxiedPlayer, true);
        } else {
            super.addJob(playerUUID, consumer);
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
            humanVerified.add(proxiedPlayer.getUniqueId());
        }

        ((JobQueue<Boolean>) jobManager.getJobQueue(JobType.IS_HUMAN)).executeJobs(proxiedPlayer, packetBotCheck.getResponse());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        humanVerified.remove(e.getPlayer().getUniqueId());
    }

    public boolean isHumanVerified(ProxiedPlayer proxiedPlayer) {
        return isHumanVerified(proxiedPlayer.getUniqueId());
    }

    public boolean isHumanVerified(UUID playerUUID) {
        return humanVerified.contains(playerUUID);
    }

}
