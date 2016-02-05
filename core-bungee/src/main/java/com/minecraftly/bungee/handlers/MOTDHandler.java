package com.minecraftly.bungee.handlers;

import com.minecraftly.bungee.handlers.job.JobManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Displays the motd to players when they join.
 */
public class MOTDHandler implements Listener {

    private static final ConfigurationProvider yamlConfigurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
    private static final List<String> defaultMessages = new ArrayList<>();

    static {
        BaseComponent[] defaultMotd1 = new ComponentBuilder("Welcome to Minecraftly Worlds.").color(ChatColor.AQUA).create();

        BaseComponent[] defaultMotd2 = new ComponentBuilder("Type ").color(ChatColor.AQUA)
                .append("/m").color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/m "))
                .append(" to message other players.").color(ChatColor.AQUA)
                .create();

        BaseComponent[] defaultMotd3 = new ComponentBuilder("MEOW rank can type ").color(ChatColor.AQUA)
                .append("/g").color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/g"))
                .append(" for global chat.").color(ChatColor.AQUA)
                .create();

        BaseComponent[] defaultMotd4 = new ComponentBuilder("Type ").color(ChatColor.AQUA)
                .append("/home").color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home"))
                .append(" to go to your world.").color(ChatColor.AQUA)
                .create();

        defaultMessages.add(ComponentSerializer.toString(defaultMotd1));
        defaultMessages.add(ComponentSerializer.toString(defaultMotd2));
        defaultMessages.add(ComponentSerializer.toString(defaultMotd3));
        defaultMessages.add(ComponentSerializer.toString(defaultMotd4));
    }

    private final JobManager jobManager;
    private final Logger logger;
    private final File motdFile;

    public MOTDHandler(JobManager jobManager, File motdFile, Logger logger) {
        this.jobManager = jobManager;
        this.motdFile = motdFile;

        try {
            motdFile.createNewFile();
            Configuration yamlConfiguration = yamlConfigurationProvider.load(motdFile);

            if (yamlConfiguration.getStringList("messages").size() == 0) {
                yamlConfiguration.set("messages", defaultMessages);
                yamlConfigurationProvider.save(yamlConfiguration, motdFile);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error loading MOTD configuration file.", e);
        }

        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPostLogin(PostLoginEvent e) {
        List<BaseComponent[]> motdComponents = getMotdFromFile();
        if (motdComponents != null) {
            ProxiedPlayer player = e.getPlayer();
            motdComponents.forEach(player::sendMessage);
        }
    }

    public List<BaseComponent[]> getMotdFromFile() {
        try {
            return yamlConfigurationProvider.load(motdFile).getStringList("messages")
                    .stream().map(ComponentSerializer::parse).collect(Collectors.toList());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading MOTD file.", e);
        }

        return null;
    }

}
