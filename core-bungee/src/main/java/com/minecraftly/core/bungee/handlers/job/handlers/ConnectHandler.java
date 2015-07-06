package com.minecraftly.core.bungee.handlers.job.handlers;

import com.minecraftly.core.bungee.handlers.job.queue.JobQueue;
import com.minecraftly.core.bungee.handlers.job.queue.ConnectJobQueue;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.logging.Logger;

/**
 * Created by Keir on 28/06/2015.
 */
public class ConnectHandler extends JobQueue<Server> implements Listener {

    private final ConnectJobQueue connectJobQueue;
    private final Logger logger;

    public ConnectHandler(ConnectJobQueue connectJobQueue, Logger logger) {
        super(Server.class);
        this.connectJobQueue = connectJobQueue;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        connectJobQueue.executeJobs(e.getPlayer(), e.getServer());
    }

}
