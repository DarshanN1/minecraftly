package com.minecraftly.core.bungee.handlers.job.handlers;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.bungee.HumanCheckManager;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.queue.HumanCheckJobQueue;
import com.minecraftly.core.packets.PacketBotCheck;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created by Keir on 28/06/2015.
 */
public class HumanCheckHandler implements Listener {

    private final JobManager jobManager;
    private final HumanCheckManager humanCheckManager;

    public HumanCheckHandler(JobManager jobManager, HumanCheckManager humanCheckManager) {
        this.jobManager = jobManager;
        this.humanCheckManager = humanCheckManager;
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
        humanCheckManager.removeHumanVerified(e.getPlayer().getUniqueId());
    }

}
