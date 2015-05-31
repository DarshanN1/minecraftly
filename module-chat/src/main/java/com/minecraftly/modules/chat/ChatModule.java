package com.minecraftly.modules.chat;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.config.DataValue;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

public class ChatModule extends Module implements Listener {

    public DataValue<Integer> CFG_LOCAL_CHAT_RADIUS = new DataValue<>(this, 100, Integer.class);

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        plugin.getConfigManager().register("chat.local-radius", CFG_LOCAL_CHAT_RADIUS);
        registerListener(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Set<Player> recipients = e.getRecipients();
        recipients.clear();
        recipients.add(player); // not sure if this is required :/
        recipients.addAll(BukkitUtilities.getNearbyPlayers(player.getLocation(), CFG_LOCAL_CHAT_RADIUS.getValue()));
    }

}
