package com.minecraftly.core.bungee;

import com.google.gson.Gson;
import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGatewayProvider;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.gateway.ProxySide;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.minecraftly.core.MinecraftlyCommon;
import com.minecraftly.core.Utilities;
import com.minecraftly.core.bungee.handlers.MOTDHandler;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.JobQueue;
import com.minecraftly.core.bungee.handlers.job.JobType;
import com.minecraftly.core.bungee.handlers.job.handlers.ConnectHandler;
import com.minecraftly.core.bungee.handlers.job.handlers.HumanCheckHandler;
import com.minecraftly.core.bungee.handlers.module.PlayerWorldsHandler;
import com.minecraftly.core.bungee.handlers.module.TpaHandler;
import lc.vq.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin implements MinecraftlyBungeeCore {

    public static final BaseComponent[] MESSAGE_NOT_HUMAN = new ComponentBuilder("You must first confirm you are human.").color(ChatColor.RED).create();

    private File configurationFile;
    private ConfigurationProvider configurationProvider;
    private Configuration configuration;

    private CommandManager commandManager;
    private ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private RedisBungeeAPI redisBungeeAPI;
    private Gson gson = new Gson();

    private final JobManager jobManager = new JobManager();
    private final HumanCheckHandler humanCheckHandler = new HumanCheckHandler(jobManager);

    @Override
    public void onEnable() {
        Utilities.createDirectory(getDataFolder());
        configurationFile = new File(getDataFolder(), "config.yml");
        configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        try {
            configurationFile.createNewFile();
            configuration = configurationProvider.load(configurationFile);
            copyDefaults();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading configuration.", e);
            return;
        }

        PluginManager pluginManager = getProxy().getPluginManager();
        gateway = BungeeGatewayProvider.getGateway(MinecraftlyCommon.GATEWAY_CHANNEL, ProxySide.SERVER, this);
        redisBungeeAPI = RedisBungee.getApi();

        PlayerWorldsHandler playerWorldsHandler = new PlayerWorldsHandler(this);
        TpaHandler tpaHandler = new TpaHandler(this);
        PreSwitchHandler preSwitchHandler = new PreSwitchHandler(gateway, getLogger());

        gateway.registerListener(playerWorldsHandler);
        gateway.registerListener(preSwitchHandler);
        gateway.registerListener(humanCheckHandler);

        commandManager = new CommandManager(this);
        commandManager.builder()
                .registerMethods(playerWorldsHandler)
                .registerMethods(tpaHandler);
        commandManager.build();

        pluginManager.registerListener(this, playerWorldsHandler);
        pluginManager.registerListener(this, tpaHandler);
        pluginManager.registerListener(this, preSwitchHandler);
        pluginManager.registerListener(this, new ConnectHandler(jobManager, getLogger()));
        pluginManager.registerListener(this, humanCheckHandler);
        pluginManager.registerListener(this, new MOTDHandler(this));

        TaskScheduler taskScheduler = getProxy().getScheduler();
        taskScheduler.schedule(this, tpaHandler, 5, TimeUnit.MINUTES);

        jobManager.addJobQueue(JobType.CONNECT, new JobQueue<>(JobType.CONNECT.getClassType()));
        jobManager.addJobQueue(JobType.IS_HUMAN, humanCheckHandler);
    }

    private void copyDefaults() {
        Configuration defaultConfiguration;

        try (InputStream inputStream = getResourceAsStream("config.yml")) {
            defaultConfiguration = configurationProvider.load(inputStream);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error copy defaults to config.", e);
            return;
        }

        boolean updated = false;

        for (String key : defaultConfiguration.getKeys()) {
            if (configuration.get(key) == null) {
                configuration.set(key, defaultConfiguration.get(key));
                updated = true;
            }
        }

        if (updated) {
            saveConfig();
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

    @Override
    public HumanCheckHandler getHumanCheckHandler() {
        return humanCheckHandler;
    }
}
