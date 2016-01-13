package com.minecraftly.core;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.UUID;

/**
 * @author iKeirNez
 */
public class PlayerWorldsRepository {

    private static final String REDIS_KEY = "mcly:world-servers";

    private JedisPool jedisPool;

    public PlayerWorldsRepository(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public boolean hasServer(UUID worldUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists(REDIS_KEY, worldUUID.toString());
        }
    }

    public String getServer(UUID worldUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(REDIS_KEY, worldUUID.toString());
        }
    }

    public void setServer(UUID worldUUID, String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (serverName != null) {
                jedis.hset(REDIS_KEY, worldUUID.toString(), serverName);
            } else {
                jedis.hdel(REDIS_KEY, worldUUID.toString());
            }
        }
    }

    public void removeAll(String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (Map.Entry<String, String> row : jedis.hgetAll(REDIS_KEY).entrySet()) {
                if (row.getValue().equals(serverName)) {
                    jedis.hdel(REDIS_KEY, row.getKey());
                }
            }
        }
    }
}
