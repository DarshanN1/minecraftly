package com.minecraftly.core.bungee.handlers;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.ikeirnez.pluginmessageframework.packet.PrimaryValuePacket;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.packets.PacketPreSwitch;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles waiting for server implementation to save before allowing a player to change server.
 */
public class PreSwitchHandler implements Listener {

    private MclyCoreBungeePlugin plugin;

    private Map<UUID, ServerInfo> savingPlayers = new HashMap<>();
    private List<UUID> savedPlayers = new ArrayList<>();

    private Map<UUID, List<Runnable>> connectJobs = new HashMap<>();

    public PreSwitchHandler(MclyCoreBungeePlugin plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, List<Runnable>> getConnectJobs() {
        return Collections.unmodifiableMap(connectJobs);
    }

    public void addJob(ProxiedPlayer proxiedPlayer, Runnable runnable) {
        addJob(proxiedPlayer.getUniqueId(), runnable);
    }

    public void addJob(UUID player, Runnable runnable) {
        List<Runnable> list = connectJobs.get(player);

        if (list == null) {
            list = new ArrayList<>();
            connectJobs.put(player, list);
        }

        list.add(runnable);
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
                    plugin.getGateway().sendPacket(player, new PrimaryValuePacket<>(PacketPreSwitch.SERVER_SAVE));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        ProxiedPlayer proxiedPlayer = e.getPlayer();
        UUID uuid = proxiedPlayer.getUniqueId();
        List<Runnable> jobs = connectJobs.get(uuid);

        if (jobs != null) {
            for (Runnable job : jobs) {
                try {
                    job.run();
                } catch (Throwable throwable1) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to run connect job for " + proxiedPlayer.getName() + " (" + uuid + ")", throwable1);
                }
            }

            connectJobs.remove(uuid);
        }

        savedPlayers.remove(uuid);
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
                    plugin.getLogger().warning("Received " + packet + " for player " + player.getName() + " when they aren't due to switch server.");
                }

                break;
        }
    }

}
