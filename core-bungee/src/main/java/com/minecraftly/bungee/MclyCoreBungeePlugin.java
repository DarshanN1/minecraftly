package com.minecraftly.bungee;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGatewayProvider;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.gateway.ProxySide;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.bungee.handlers.HeartbeatTask;
import com.minecraftly.MinecraftlyCommon;
import com.minecraftly.bungee.handlers.MOTDHandler;
import com.minecraftly.bungee.handlers.PreSwitchHandler;
import com.minecraftly.bungee.handlers.RedisMessagingHandler;
import com.minecraftly.bungee.handlers.SlaveHandler;
import com.minecraftly.bungee.handlers.job.JobManager;
import com.minecraftly.bungee.handlers.job.handlers.ConnectHandler;
import com.minecraftly.bungee.handlers.job.queue.ConnectJobQueue;
import com.minecraftly.bungee.handlers.module.PlayerWorldsHandler;
import com.minecraftly.PlayerWorldsRepository;
import com.minecraftly.bungee.handlers.module.tpa.TpaData;
import com.minecraftly.bungee.handlers.module.tpa.TpaDataAdapter;
import com.minecraftly.bungee.handlers.module.tpa.TpaHandler;
import com.minecraftly.bungee.utilities.BungeeUtilities;
import com.minecraftly.redis.RedisHelper;
import com.minecraftly.redis.message.ServerInstanceData;
import com.minecraftly.redis.message.gson.ServerDataAdapter;
import com.minecraftly.utilities.ComputeEngineHelper;
import com.minecraftly.utilities.Utilities;
import net.ellune.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin implements MinecraftlyBungeeCore {

    private static MclyCoreBungeePlugin instance;

    public static MclyCoreBungeePlugin getInstance() {
        return instance;
    }

    // Google Compute
    private String computeUniqueId;

    private File configurationFile;
    private ConfigurationProvider configurationProvider;
    private Configuration configuration;

    private CommandManager commandManager;
    private ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private RedisBungeeAPI redisBungeeAPI;
    private JedisPool jedisPool;
    private Gson gson;

    private SlaveHandler slaveHandler;
    private final JobManager jobManager = new JobManager();

    @Override
    public void onLoad() {
        instance = this;
        gson = new GsonBuilder()
                .registerTypeAdapter(ServerInstanceData.class, new ServerDataAdapter())
                .registerTypeAdapter(TpaData.class, new TpaDataAdapter())
                .create();
    }

    @Override
    public void onEnable() {
        PluginManager pluginManager = getProxy().getPluginManager();
        TaskScheduler taskScheduler = getProxy().getScheduler();

        Utilities.createDirectory(getDataFolder());
        configurationFile = new File(getDataFolder(), "config.yml");
        configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        try {
            configurationFile.createNewFile();
            configuration = configurationProvider.load(configurationFile);
            BungeeUtilities.copyDefaultsFromJarFile(configuration, "config.yml", configurationProvider, configurationFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading configuration.", e);
            return;
        }

        redisBungeeAPI = RedisBungee.getApi();

        if (redisBungeeAPI == null) {
            getLogger().severe("RedisBungeeAPI is not available.");
            return;
        }

        try {
            String configUniqueId = configuration.getString("debug.uniqueId");
            computeUniqueId = configUniqueId.equals("-1") ? ComputeEngineHelper.queryUniqueId() : configUniqueId;
            getLogger().info("Instance ID - " + computeUniqueId);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error querying Compute API.", e);
            return;
        }

        int heartbeatPort = configuration.getInt("debug.heartbeatPort");

        try {
            taskScheduler.schedule(this, new HeartbeatTask(heartbeatPort), 0, MinecraftlyCommon.UDP_HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
        } catch (SocketException e) {
            getLogger().log(Level.SEVERE, "Error initializing UDP socket (" + heartbeatPort + ").", e);
            return;
        }

        Map<String, ServerInfo> servers = getProxy().getServers();
        servers.put(computeUniqueId, getProxy().constructServerInfo(computeUniqueId, new InetSocketAddress("localhost", 1), null, false)); // put a placeholder in for now

        try {
            BungeeUtilities.setListenerInfoField("defaultServer", computeUniqueId);
            BungeeUtilities.setListenerInfoField("fallbackServer", computeUniqueId); // must be set so we don't get NPEs
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Error whilst applying reflection for default server.", e);
            return;
        }

        FutureTask<JedisPool> jedisTask = new FutureTask<>(() ->
                connectRedis(configuration.getString("redis.host"), configuration.getInt("redis.port"), configuration.getString("redis.password")));

        taskScheduler.runAsync(this, jedisTask);

        try {
            jedisPool = jedisTask.get();
            testRedisConnection(jedisPool);
            getLogger().info("Connected to Redis server.");
        } catch (InterruptedException | ExecutionException e) {
            getLogger().log(Level.SEVERE, "Error whilst connecting to Redis.", e);
            return;
        }

        gateway = BungeeGatewayProvider.getGateway(MinecraftlyCommon.GATEWAY_CHANNEL, ProxySide.SERVER, this);
        slaveHandler = new SlaveHandler(gson, jedisPool, getLogger(), String.valueOf(computeUniqueId));
        pluginManager.registerListener(this, slaveHandler);
        taskScheduler.schedule(this, slaveHandler, RedisHelper.HEARTBEAT_INTERVAL, RedisHelper.HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
        slaveHandler.initialize();

        redisBungeeAPI.registerPubSubChannels(RedisMessagingHandler.MESSAGE_PLAYER_CHANNEL, ServerInstanceData.CHANNEL, RedisHelper.CHANNEL_SERVER_GOING_DOWN);
        pluginManager.registerListener(this, new RedisMessagingHandler());

        ConnectJobQueue connectJobQueue = new ConnectJobQueue();
        jobManager.addJobQueue(connectJobQueue);
        pluginManager.registerListener(this, new ConnectHandler(this, connectJobQueue));

        PlayerWorldsHandler playerWorldsHandler = new PlayerWorldsHandler(gateway, jobManager, new PlayerWorldsRepository(jedisPool), redisBungeeAPI);
        TpaHandler tpaHandler = new TpaHandler(this);
        PreSwitchHandler preSwitchHandler = new PreSwitchHandler(gateway, getLogger());

        gateway.registerListener(playerWorldsHandler);
        gateway.registerListener(preSwitchHandler);

        commandManager = new CommandManager(this);
        commandManager.builder()
                .registerMethods(playerWorldsHandler)
                .registerMethods(tpaHandler);
        commandManager.build();

        pluginManager.registerListener(this, playerWorldsHandler);
        pluginManager.registerListener(this, tpaHandler);
        pluginManager.registerListener(this, preSwitchHandler);
        pluginManager.registerListener(this, new MOTDHandler(jobManager, new File(getDataFolder(), "motd.yml"), getLogger()));

        taskScheduler.schedule(this, tpaHandler, 5, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable() {
        jedisPool.destroy();
        instance = null;
    }

    private JedisPool connectRedis(String host, int port, String password) {
        Preconditions.checkNotNull(host, "Host cannot be null.");
        Preconditions.checkArgument(!host.isEmpty(), "Host cannot be empty.");
        Preconditions.checkArgument(port >= 1 && port <= 65535, "Port must be in range 1-65535");

        if (password != null && (password .isEmpty() || password.equals("none"))) {
            password = null;
        }

        JedisPoolConfig config = new JedisPoolConfig();
        return new JedisPool(config, host, port, 0, password);
    }

    private void testRedisConnection(JedisPool jedisPool) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        } catch (JedisConnectionException e) {
            jedisPool.destroy();
            throw e;
        }
    }

    private void saveConfig() {
        try {
            configurationProvider.save(configuration, configurationFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error saving configuration.", e);
        }
    }

    @Override
    public String getComputeUniqueId() {
        return computeUniqueId;
    }

    @Override
    public ProxyGateway<ProxiedPlayer, Server, ServerInfo> getGateway() {
        return gateway;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public RedisBungeeAPI getRedisBungeeAPI() {
        return redisBungeeAPI;
    }

    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public JobManager getJobManager() {
        return jobManager;
    }
}
