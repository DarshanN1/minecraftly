package com.minecraftly.core.bungee.handlers;

import com.google.gson.Gson;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.Jedis;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.JedisPool;
import com.minecraftly.core.redis.RedisHelper;
import com.minecraftly.core.redis.message.ServerInstanceData;
import com.minecraftly.core.utilities.Utilities;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 17/08/2015.
 */
public class SlaveHandler implements Listener, Runnable {

    private Gson gson;
    private JedisPool jedisPool;
    private Logger logger;

    public SlaveHandler(Gson gson, JedisPool jedisPool, Logger logger) {
        this.gson = gson;
        this.jedisPool = jedisPool;
        this.logger = logger;
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

        if (servers.size() == 1 && servers.containsKey("dummy-server")) { // bungee doesn't like to startup with no servers, crappy workaround
            servers.remove("dummy-server");

            for (ListenerInfo listenerInfo : proxyServer.getConfig().getListeners()) {
                try { // hacky reflection -_-
                    Field defaultServerField = ListenerInfo.class.getDeclaredField("defaultServer");
                    defaultServerField.setAccessible(true);
                    Utilities.removeFinal(defaultServerField);
                    defaultServerField.set(listenerInfo, id);

                    Field fallbackServerField = ListenerInfo.class.getDeclaredField("fallbackServer");
                    fallbackServerField.setAccessible(true);
                    Utilities.removeFinal(fallbackServerField);
                    fallbackServerField.set(listenerInfo, id);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.log(Level.SEVERE, "Exception whilst applying reflection for default and fallback server.", e);
                }
            }
        }

        servers.put(id, proxyServer.constructServerInfo(id, socketAddress, null, false));
    }

    @EventHandler
    public void onNewServer(PubSubMessageEvent e) {
        String message = e.getMessage();

        switch (e.getChannel()) {
            default: break;
            case ServerInstanceData.CHANNEL:
                ServerInstanceData serverInstanceData = gson.fromJson(message, ServerInstanceData.class);
                long id = serverInstanceData.getId();
                InetSocketAddress inetSocketAddress = serverInstanceData.getSocketAddress();

                logger.info("New server - " + id + " (" + inetSocketAddress.toString() + ").");
                addNewServer(String.valueOf(id), inetSocketAddress);
                break;
            case RedisHelper.CHANNEL_SERVER_GOING_DOWN:
                ProxyServer.getInstance().getServers().remove(message);
                break;
        }
    }

    @Override
    public void run() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> heartbeats = jedis.hgetAll("mcly:heartbeats");

            for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
                long id = Long.parseLong(entry.getKey());
                long lastHeartbeat = Long.parseLong(entry.getValue());
                long lastHeartbeatDifference = System.currentTimeMillis() - lastHeartbeat;

                if (lastHeartbeatDifference > TimeUnit.SECONDS.toMillis(RedisHelper.HEARTBEAT_INTERVAL + 5)) { // +5 = tolerance
                    logger.warning("Server instance '" + id + "' seems to have gone down (or system clock incorrect). Not seen for " + TimeUnit.MILLISECONDS.toSeconds(lastHeartbeatDifference));
                }
            }

            Iterator<Map.Entry<String, ServerInfo>> iterator = ProxyServer.getInstance().getServers().entrySet().iterator();

            while (iterator.hasNext()) { // remove old servers
                Map.Entry<String, ServerInfo> entry = iterator.next();

                if (!heartbeats.containsKey(entry.getKey())) {
                    iterator.remove();
                }
            }
        }
    }

}
