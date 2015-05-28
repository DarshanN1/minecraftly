package com.minecraftly.modules.chat;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

public class ChatModule extends Module implements Listener {

    public static final int CHAT_RADIUS = 100;

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        registerListener(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Set<Player> recipients = e.getRecipients();
        recipients.clear();
        recipients.add(player); // not sure if this is required :/
        recipients.addAll(BukkitUtilities.getNearbyPlayers(player.getLocation(), CHAT_RADIUS));
    }

}
