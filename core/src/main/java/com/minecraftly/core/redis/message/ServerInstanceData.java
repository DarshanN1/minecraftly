package com.minecraftly.core.redis.message;

import java.net.InetSocketAddress;

/**
 * Created by Keir on 28/08/2015.
 */
public class ServerInstanceData {

    public static final String CHANNEL = "NewServer";

    private long id;
    private InetSocketAddress inetSocketAddress;

    public ServerInstanceData(long id, InetSocketAddress inetSocketAddress) {
        this.id = id;
        this.inetSocketAddress = inetSocketAddress;
    }

    public long getId() {
        return id;
    }

    public InetSocketAddress getSocketAddress() {
        return inetSocketAddress;
    }
}
