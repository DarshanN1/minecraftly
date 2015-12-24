package com.minecraftly.core.bungee.handlers.job.handlers;

import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.bungee.handlers.job.queue.JobQueue;
import com.minecraftly.core.bungee.handlers.job.queue.ConnectJobQueue;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.concurrent.TimeUnit;

/**
 * Created by Keir on 28/06/2015.
 */
public class ConnectHandler extends JobQueue<Server> implements Listener {

    private final MclyCoreBungeePlugin plugin;
    private final ConnectJobQueue connectJobQueue;

    public ConnectHandler(MclyCoreBungeePlugin plugin, ConnectJobQueue connectJobQueue) {
        super(Server.class);
        this.plugin = plugin;
        this.connectJobQueue = connectJobQueue;
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            connectJobQueue.executeJobs(e.getPlayer(), e.getServer());
        }, 1, TimeUnit.SECONDS);
    }

}
