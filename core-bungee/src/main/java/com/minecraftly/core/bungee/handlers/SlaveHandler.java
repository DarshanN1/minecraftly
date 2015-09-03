package com.minecraftly.core.bungee.handlers;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.Jedis;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.JedisPool;
import com.minecraftly.core.redis.RedisHelper;
import com.minecraftly.core.redis.message.ServerInstanceData;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by Keir on 17/08/2015.
 */
public class SlaveHandler implements Listener, Runnable {

    private Gson gson;
    private JedisPool jedisPool;
    private Logger logger;
    private String mainServerId;

    public SlaveHandler(Gson gson, JedisPool jedisPool, Logger logger, String mainServerName) {
        this.gson = gson;
        this.jedisPool = jedisPool;
        this.logger = logger;
        this.mainServerId = mainServerName;
    }

    public void initialize() {
        logger.info("Initializing servers.");
        int added = 0;

        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> servers = jedis.hgetAll("mcly:instance_addresses"); // id, address

            for (Map.Entry<String, String> entry : servers.entrySet()) {
                String id = entry.getKey();
                String[] addressParts = entry.getValue().split(":");

                if (addressParts.length != 2) {
                    logger.severe("Address doesn't have exactly 2 parts to it: " + entry.getValue());
                }

                InetSocketAddress socketAddress = new InetSocketAddress(addressParts[0], Integer.parseInt(addressParts[1]));
                addNewServer(id, socketAddress);
                added++;
            }
        }

        logger.info("Found " + added + " servers.");
    }

    public void addNewServer(String id, InetSocketAddress socketAddress) {
        ProxyServer proxyServer = ProxyServer.getInstance();
        Map<String, ServerInfo> servers = proxyServer.getServers();
        boolean sameInstance = id.equals(mainServerId);
        String motd = "";

        if (sameInstance) {
            if (!socketAddress.getHostString().equals("localhost")) {
                socketAddress = new InetSocketAddress("localhost", socketAddress.getPort());
            }

            // set motd (have to do this manually)
            for (ListenerInfo listenerInfo : proxyServer.getConfig().getListeners()) {
                motd = listenerInfo.getMotd();

                if (motd != null && !motd.isEmpty()) {
                    break;
                }
            }
        }

        servers.put(id, proxyServer.constructServerInfo(id, socketAddress, motd, false));
    }

    @EventHandler
    public void onNewServer(PubSubMessageEvent e) {
        String message = e.getMessage();

        switch (e.getChannel()) {
            default: break;
            case ServerInstanceData.CHANNEL:
                ServerInstanceData serverInstanceData = gson.fromJson(message, ServerInstanceData.class);
                String id = serverInstanceData.getId();
                InetSocketAddress inetSocketAddress = serverInstanceData.getSocketAddress();

                logger.info("New server - " + id + " (" + inetSocketAddress.toString() + ").");
                addNewServer(id, inetSocketAddress);
                break;
            case RedisHelper.CHANNEL_SERVER_GOING_DOWN:
                if (!message.equals(mainServerId)) {
                    ProxyServer.getInstance().getServers().remove(message);
                }

                break;
        }
    }

    @Override
    public void run() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> heartbeats = jedis.hgetAll("mcly:heartbeats");

            for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                String id = entry.getKey();
                long lastHeartbeat = Long.parseLong(entry.getValue());
                long lastHeartbeatDifference = System.currentTimeMillis() - lastHeartbeat;

                if (lastHeartbeatDifference > TimeUnit.SECONDS.toMillis(RedisHelper.HEARTBEAT_INTERVAL + 5)) { // +5 = tolerance
                    // todo remove?
                    logger.warning("Server instance '" + id + "' seems to have gone down (or system clock incorrect). Not seen for " + TimeUnit.MILLISECONDS.toSeconds(lastHeartbeatDifference));
                }
            }

            Iterator<Map.Entry<String, ServerInfo>> iterator = ProxyServer.getInstance().getServers().entrySet().iterator();

            while (iterator.hasNext()) { // remove old servers
                Map.Entry<String, ServerInfo> entry = iterator.next();
                String serverId = entry.getKey();

                if (!heartbeats.containsKey(serverId) && !serverId.equals(mainServerId) && !serverId.equals("dummy-server")) { // main server always exists
                    iterator.remove();
                }
            }
        }
    }

}
