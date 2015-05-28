package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGatewayProvider;
import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.gateway.ProxySide;
import com.minecraftly.core.MinecraftlyCommon;
import com.minecraftly.core.Utilities;
import com.minecraftly.core.bungee.handlers.module.SpawnHandler;
import com.minecraftly.core.bungee.handlers.module.SurvivalWorldsHandler;
import com.sk89q.intake.Command;
import lc.vq.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin implements MinecraftyBungeeCore {

    private File configurationFile;
    private ConfigurationProvider configurationProvider;
    private Configuration configuration;

    private CommandManager commandManager;
    private ProxyGateway<ProxiedPlayer, ServerInfo> gateway;

    @Override
    public void onEnable() {
        configurationFile = new File(getDataFolder(), "config.yml");
        Utilities.createDirectory(getDataFolder());
        configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        try {
            configurationFile.createNewFile();
            configuration = configurationProvider.load(configurationFile);
            copyDefaults();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading configuration.", e);
            return;
        }

        SurvivalWorldsHandler survivalWorldsHandler = new SurvivalWorldsHandler(this);

        gateway = BungeeGatewayProvider.getGateway(MinecraftlyCommon.GATEWAY_CHANNEL, ProxySide.SERVER, this);
        gateway.registerListener(survivalWorldsHandler);

        commandManager = new CommandManager(this);
        commandManager.builder()
                .registerMethods(this)
                .registerMethods(new SpawnHandler(this))
                .registerMethods(survivalWorldsHandler);
        commandManager.build();

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, survivalWorldsHandler);
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
    public ProxyGateway<ProxiedPlayer, ServerInfo> getGateway() {
        return gateway;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Command(aliases = "mclybungeetestcommand", desc = "A test command.")
    public void testCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent("Intake is working in the MinecraftlyCore Bungee plugin :D"));
    }

}
