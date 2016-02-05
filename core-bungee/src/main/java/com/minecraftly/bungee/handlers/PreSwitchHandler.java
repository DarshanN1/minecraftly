package com.minecraftly.bungee.handlers;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.ikeirnez.pluginmessageframework.packet.PrimaryValuePacket;
import com.minecraftly.packets.PacketPreSwitch;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by Keir on 28/06/2015.
 */
public class PreSwitchHandler implements Listener {

    private final ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway;
    private final Logger logger;

    private final Map<UUID, ServerInfo> savingPlayers = new HashMap<>();
    private final List<UUID> savedPlayers = new ArrayList<>();

    public PreSwitchHandler(ProxyGateway<ProxiedPlayer, Server, ServerInfo> gateway, Logger logger) {
        this.gateway = gateway;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        Server currentServer = player.getServer();

        if (currentServer != null && currentServer.getInfo() != e.getTarget()) {
            if (!savedPlayers.contains(uuid)) {
                e.setCancelled(true);

                if (!savingPlayers.containsKey(uuid)) { // prevent multiple saves
                    savingPlayers.put(uuid, e.getTarget());
                    player.sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Please wait whilst we save your data...").color(ChatColor.AQUA).create());
                    gateway.sendPacket(player, new PrimaryValuePacket<>(PacketPreSwitch.SERVER_SAVE));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        savedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @PacketHandler
    public void onProxySwitch(final ProxiedPlayer player, PacketPreSwitch packet) {
        final UUID uuid = player.getUniqueId();

        switch (packet) {
            default: throw new UnsupportedOperationException("Don't know how to handle: " + packet + ".");
            case PROXY_SWITCH:
                if (savingPlayers.containsKey(uuid)) {
                    savedPlayers.add(uuid);
                    player.connect(savingPlayers.get(uuid));
                    savingPlayers.remove(uuid);
                } else {
                    logger.warning("Received " + packet + " for player " + player.getName() + " when they aren't due to switch server.");
                }

                break;
        }
    }

}
