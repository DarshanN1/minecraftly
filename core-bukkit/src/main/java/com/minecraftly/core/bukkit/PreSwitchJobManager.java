package com.minecraftly.core.bukkit;

import com.ikeirnez.pluginmessageframework.packet.PacketHandler;
import com.minecraftly.core.Callback;
import com.minecraftly.core.packets.PacketPreSwitch;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages storing an executing of pre switch jobs
 */
public class PreSwitchJobManager {

    private List<Callback<Player>> jobs = new ArrayList<>();

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
            task.call(player);
        }
    }

    @PacketHandler
    public void onPlayerPreSwitch(Player player, PacketPreSwitch packetType) {
        System.out.println("PRESWITCH RECEIVED"); // todo remove

        switch (packetType) {
            default: throw new UnsupportedOperationException("Don't know how to handle: " + packetType + ".");
            case SERVER_SAVE:
                executeJobs(player);
                break;
        }
    }

}
