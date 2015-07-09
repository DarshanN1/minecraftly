package com.minecraftly.core.bungee.handlers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.minecraftly.core.bungee.handlers.job.JobManager;
import com.minecraftly.core.bungee.handlers.job.queue.HumanCheckJobQueue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays the motd to players when they join.
 */
public class MOTDHandler implements Listener {

    private final JobManager jobManager;
    private final Logger logger;

    private File motdFile;

    public MOTDHandler(JobManager jobManager, File motdFile, Logger logger) {
        this.jobManager = jobManager;
        this.logger = logger;

        this.motdFile = motdFile;

        if (!this.motdFile.exists()) {
            try {
                BaseComponent[] defaultMotd = new ComponentBuilder("Welcome to Minecraftly Worlds.").color(ChatColor.AQUA)
                        .append("\n")
                        .append("Type ").color(ChatColor.AQUA)
                            .append("/m").color(ChatColor.GOLD)
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/m "))
                            .append(" to message other players.").color(ChatColor.AQUA)
                            .append("\n")
                        .append("MEOW rank can type ").color(ChatColor.AQUA)
                            .append("/g").color(ChatColor.GOLD)
                                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/g"))
                            .append(" for global chat.").color(ChatColor.AQUA)
                            .append("\n")
                        .append("Type ").color(ChatColor.AQUA)
                            .append("/home").color(ChatColor.GOLD)
                                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home"))
                            .append(" to go to your world.").color(ChatColor.AQUA)
                        .create();

                this.motdFile.createNewFile();
                Files.write(this.motdFile.toPath(), Collections.singletonList(prettifyJson(ComponentSerializer.toString(defaultMotd))));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Couldn't write default motd file.", e);
            }
        }
    }

    public String prettifyJson(String json) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(json));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPostLogin(PostLoginEvent e) {
        jobManager.getJobQueue(HumanCheckJobQueue.class).addJob(e.getPlayer(), ((proxiedPlayer, human) -> {
            if (human) {
                BaseComponent[] motdComponents = getMotdFromFile();
                if (motdComponents != null) {
                    proxiedPlayer.sendMessage(motdComponents);
                }
            }
        }));
    }

    public BaseComponent[] getMotdFromFile() {
        try {
            String data = "";
            for (String line : Files.readAllLines(motdFile.toPath())) {
                data += line;
            }

            return ComponentSerializer.parse(data);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading MOTD file.", e);
        }

        return null;
    }

}
