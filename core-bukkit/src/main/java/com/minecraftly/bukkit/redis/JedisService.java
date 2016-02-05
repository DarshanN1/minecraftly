package com.minecraftly.bukkit.redis;

import com.google.gson.Gson;
import com.minecraftly.redis.RedisHelper;
import com.minecraftly.redis.message.ServerInstanceData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.InetSocketAddress;

/**
 * Created by Keir on 01/09/2015.
 */
public class JedisService {

    private JedisPool jedisPool;
    private String computeUniqueId;
    private InetSocketAddress instanceExternalSocketAddress;

    public JedisService(String computeUniqueId, InetSocketAddress instanceExternalSocketAddress, String jedisHost, int jedisPort, String jedisPassword) {
        if (instanceExternalSocketAddress == null) {
            throw new IllegalArgumentException("InstanceExternalSocketAddress cannot be null.");
        }

        if (jedisHost == null || jedisHost.isEmpty()) {
            throw new IllegalArgumentException("Host is not defined.");
        }

        if (jedisPassword != null && jedisPassword.isEmpty()) {
            jedisPassword = null;
        }

        this.computeUniqueId = computeUniqueId;
        this.instanceExternalSocketAddress = instanceExternalSocketAddress;

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1);
        jedisPoolConfig.setJmxEnabled(false);

        try {
            jedisPool = new JedisPool(jedisPoolConfig, jedisHost, jedisPort, 0, jedisPassword);
        } catch (JedisConnectionException e) {
            jedisPool.destroy();
            jedisPool = null;
            throw e;
        }
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void heartbeat() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset("mcly:heartbeats", computeUniqueId, String.valueOf(System.currentTimeMillis()));
        }
    }

    public void instanceAlive(Gson gson) {
        heartbeat();

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset("mcly:instance_addresses", computeUniqueId, instanceExternalSocketAddress.toString());

            ServerInstanceData serverInstanceData = new ServerInstanceData(computeUniqueId, instanceExternalSocketAddress);
            jedis.publish(ServerInstanceData.CHANNEL, gson.toJson(serverInstanceData));
        }
    }

    public void destroy() {
        try (Jedis jedis = jedisPool.getResource()) {
            String idString = String.valueOf(computeUniqueId);
            jedis.hdel("mcly:heartbeats", idString);
            jedis.hdel("mcly:instance_addresses", idString);
            jedis.publish(RedisHelper.CHANNEL_SERVER_GOING_DOWN, idString);
        }
    }
}
