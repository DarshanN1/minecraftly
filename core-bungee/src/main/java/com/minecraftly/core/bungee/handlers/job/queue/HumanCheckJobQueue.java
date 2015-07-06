package com.minecraftly.core.bungee.handlers.job.queue;

import com.minecraftly.core.bungee.HumanCheckManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Created by Keir on 06/07/2015.
 */
public class HumanCheckJobQueue extends JobQueue<Boolean> {

    private final HumanCheckManager humanCheckManager;

    public HumanCheckJobQueue(HumanCheckManager humanCheckManager) {
        super(Boolean.class);
        this.humanCheckManager = humanCheckManager;
    }

    @Override
    public void addJob(UUID playerUUID, BiConsumer<ProxiedPlayer, Boolean> consumer) {
        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerUUID);

        if (humanCheckManager.isHumanVerified(playerUUID) && proxiedPlayer != null) { // short-circuit
            consumer.accept(proxiedPlayer, true);
        } else {
            super.addJob(playerUUID, consumer);
        }
    }
}
