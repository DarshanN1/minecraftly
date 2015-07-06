package com.minecraftly.core.bungee.handlers.job.queue;

import net.md_5.bungee.api.connection.Server;

/**
 * Created by Keir on 06/07/2015.
 */
public class ConnectJobQueue extends JobQueue<Server> {

    public ConnectJobQueue() {
        super(Server.class);
    }

}
