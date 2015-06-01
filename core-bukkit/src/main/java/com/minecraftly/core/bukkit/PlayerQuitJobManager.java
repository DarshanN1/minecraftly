package com.minecraftly.core.bukkit;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.ikeirnez.pluginmessageframework.packet.PrimaryValuePacket;
import com.minecraftly.core.Callback;
import com.minecraftly.core.packets.PacketPreSwitch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages storing an executing of pre switch jobs
 */
public class PlayerQuitJobManager implements Listener {

    private static final int TOLERANCE_SECONDS = 20;

    private ServerGateway<Player> gateway;

    private Map<UUID, Long> preSwitchJobExecuteTimes = new HashMap<>();
    private List<Callback<Player>> jobs = new ArrayList<>();

    public PlayerQuitJobManager(Plugin plugin, ServerGateway<Player> gateway) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.gateway = gateway;
        this.gateway.registerListener(this);
    }

    public List<Callback<Player>> getJobs() {
        return Collections.unmodifiableList(jobs);
    }

    public void addJob(Callback<Player> task) {
        jobs.add(task);
    }

    public void removeJob(Callback<Player> task) {
        jobs.remove(task);
    }

    public void executeJobs(Player player) {
        for (Callback<Player> task : jobs) {
            task.call(player); // todo execute async?
        }

        preSwitchJobExecuteTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @PacketHandler
    public void onPlayerPreSwitch(Player player, PacketPreSwitch packetType) {
        switch (packetType) {
            default: throw new UnsupportedOperationException("Don't know how to handle: " + packetType + ".");
            case SERVER_SAVE:
                executeJobs(player);
                gateway.sendPacket(player, new PrimaryValuePacket<>(PacketPreSwitch.PROXY_SWITCH));
                break;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!preSwitchJobExecuteTimes.containsKey(uuid)
                || preSwitchJobExecuteTimes.get(uuid) + TimeUnit.SECONDS.toMillis(TOLERANCE_SECONDS) < System.currentTimeMillis()) {
            executeJobs(player);
        }

        preSwitchJobExecuteTimes.remove(uuid);
    }

}
