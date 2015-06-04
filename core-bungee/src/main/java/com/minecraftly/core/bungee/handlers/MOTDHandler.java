package com.minecraftly.core.bungee.handlers;

import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
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
        this.motdFile = new File(plugin.getDataFolder(), "motd.txt");

        if (!this.motdFile.exists()) {
            try {
                BaseComponent[] defaultMotd = new ComponentBuilder(
                        "This is the first chat message line.")
                            .color(ChatColor.AQUA)
                        .append("\n")
                        .append("This is the second chat message line.")
                            .color(ChatColor.AQUA)
                            .bold(true)
                        .append("\n")
                        .append("Now lets have some fun!")
                            .color(ChatColor.GOLD)
                        .append("\n")
                        .append("Clickable link with hover text: ")
                            .color(ChatColor.AQUA)
                            .append("here")
                                .color(ChatColor.GOLD)
                                .event(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        new ComponentBuilder("This is some hover text!")
                                                .color(ChatColor.RED)
                                                .create()))
                                .event(new ClickEvent(
                                        ClickEvent.Action.OPEN_URL,
                                        "http://mc.ly"
                                ))
                        .create();

                this.motdFile.createNewFile();
                // todo convert to pretty json?
                Files.write(this.motdFile.toPath(), Collections.singletonList(ComponentSerializer.toString(defaultMotd)));
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't write default motd file.", e);
            }
        }
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
