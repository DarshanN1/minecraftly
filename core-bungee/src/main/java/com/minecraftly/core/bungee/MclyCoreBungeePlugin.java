package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.bungeecord.BungeeGateway;
import com.ikeirnez.pluginmessageframework.bungeecord.DefaultBungeeGateway;
import com.ikeirnez.pluginmessageframework.connection.ProxySide;
import com.minecraftly.core.MinecraftlyCommon;
import com.minecraftly.core.Utilities;
import com.minecraftly.core.bungee.module.SpawnModuleHandler;
import com.sk89q.intake.Command;
import lc.vq.exhaust.bungee.command.CommandManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Created by Keir on 24/03/2015.
 */
public class MclyCoreBungeePlugin extends Plugin implements MinecraftyBungeeCore {

    private File configurationFile = new File(getDataFolder(), "config.yml");
    private ConfigurationProvider configurationProvider;
    private Configuration configuration;

    private CommandManager commandManager;
    private BungeeGateway gateway;

    @Override
    public void onEnable() {
        Utilities.createDirectory(getDataFolder());
        configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        try {
            configurationFile.createNewFile();
            configuration = configurationProvider.load(configurationFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading configuration.", e);
            return;
        }

        gateway = new DefaultBungeeGateway(MinecraftlyCommon.GATEWAY_CHANNEL, ProxySide.SERVER, this);
        gateway.registerListener(new SpawnModuleHandler(this));

        commandManager = new CommandManager(this);
        commandManager.builder().registerMethods(this);
        commandManager.build();
    }

    @Override
    public BungeeGateway getGateway() {
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
