package com.minecraftly.core.bungee.handlers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.logging.Level;

/**
 * Displays the motd to players when they join.
 */
public class MOTDHandler implements Listener {

    private final MclyCoreBungeePlugin plugin;
    private File motdFile;

    public MOTDHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
        this.motdFile = new File(plugin.getDataFolder(), "motd.json");

        if (!this.motdFile.exists()) {
            try {
                BaseComponent[] defaultMotd = new ComponentBuilder("Welcome to Minecraftly Homes.").color(ChatColor.AQUA)
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
                            .append(" to go to your home.").color(ChatColor.AQUA)
                        .create();

                this.motdFile.createNewFile();
                Files.write(this.motdFile.toPath(), Collections.singletonList(prettifyJson(ComponentSerializer.toString(defaultMotd))));
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't write default motd file.", e);
            }
        }
    }

    public String prettifyJson(String json) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(json));
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent e) {
        BaseComponent[] motdComponents = getMotdFromFile();
        if (motdComponents != null) {
            e.getPlayer().sendMessage(motdComponents);
        }
    }

    public BaseComponent[] getMotdFromFile() {
        try {
            String data = "";
            for (String line : Files.readAllLines(motdFile.toPath())) {
                data += line;
            }

            return ComponentSerializer.parse(data);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reading MOTD file.", e);
        }

        return null;
    }

}
