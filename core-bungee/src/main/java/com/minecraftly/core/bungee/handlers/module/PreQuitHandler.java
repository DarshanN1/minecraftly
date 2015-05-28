package com.minecraftly.core.bungee.handlers.module;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.ikeirnez.pluginmessageframework.packet.PrimaryValuePacket;
import com.minecraftly.core.bungee.MinecraftlyBungeeCore;
import com.minecraftly.core.packets.PacketPreSwitch;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles waiting for server implementation to save before allowing a player to change server.
 */
public class PreQuitHandler implements Listener {

    private MinecraftlyBungeeCore plugin;

    private Map<UUID, ServerInfo> savingPlayers = new HashMap<>();
    private List<UUID> savedPlayers = new ArrayList<>();

    public PreQuitHandler(MinecraftlyBungeeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        Server currentServer = player.getServer();

        if (currentServer != null) {
            if (!savedPlayers.contains(uuid)) {
                e.setCancelled(true);

                if (!savingPlayers.containsKey(uuid)) { // prevent multiple saves
                    savingPlayers.put(uuid, e.getTarget());
                    player.sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Please wait whilst we save your data...").color(ChatColor.AQUA).create());
                    plugin.getGateway().sendPacket(player, new PrimaryValuePacket<>(PacketPreSwitch.SERVER_SAVE));
                }
            } else {
                savedPlayers.remove(uuid);
            }
        }
    }

    @PacketHandler
    public void onProxySwitch(ProxiedPlayer player, PacketPreSwitch packet) {
        UUID uuid = player.getUniqueId();

        switch (packet) {
            default: throw new UnsupportedOperationException("Don't know how to handle: " + packet + ".");
            case PROXY_SWITCH:
                if (savingPlayers.containsKey(uuid)) {
                    savedPlayers.add(uuid);
                    player.connect(savingPlayers.get(uuid));
                    savingPlayers.remove(uuid);
                }

                break;
        }
    }

}
