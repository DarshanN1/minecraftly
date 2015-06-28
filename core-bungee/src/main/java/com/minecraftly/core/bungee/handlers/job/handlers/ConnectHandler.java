package com.minecraftly.core.bungee.handlers.job.handlers;

import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.JobQueue;
import com.minecraftly.core.bungee.handlers.job.JobType;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.logging.Logger;

/**
 * Created by Keir on 28/06/2015.
 */
public class ConnectHandler implements Listener {

    private final JobManager jobManager;
    private final Logger logger;

    public ConnectHandler(JobManager jobManager, Logger logger) {
        this.jobManager = jobManager;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        ((JobQueue<Server>) jobManager.getJobQueue(JobType.CONNECT)).executeJobs(e.getPlayer(), e.getServer());
    }

}
