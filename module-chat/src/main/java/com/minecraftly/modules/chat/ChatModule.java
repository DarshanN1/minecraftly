package com.minecraftly.modules.chat;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

/**
 * Created by Keir on 05/04/2015.
 */
public class ChatModule extends Module implements Listener {

    public static final int CHAT_RADIUS = 100;

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Set<Player> recipients = e.getRecipients();
        recipients.clear();
        recipients.add(player); // not sure if this is required :/

        for (Entity entity : player.getNearbyEntities(CHAT_RADIUS, CHAT_RADIUS, CHAT_RADIUS)) {
            if (entity.getType() == EntityType.PLAYER) {
                recipients.add((Player) entity);
            }
        }
    }

}
