package com.minecraftly.core.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Keir on 06/07/2015.
 */
public class HumanCheckManager {

    //private final List<UUID> humanVerified = new ArrayList<>();

    public boolean isHumanVerified(ProxiedPlayer proxiedPlayer) {
        return isHumanVerified(proxiedPlayer.getUniqueId());
    }

    public boolean isHumanVerified(UUID playerUUID) {
        return true;
        //return humanVerified.contains(playerUUID);
    }

    public void addHumanVerified(ProxiedPlayer proxiedPlayer) {
        addHumanVerified(proxiedPlayer.getUniqueId());
    }

    public void addHumanVerified(UUID playerUUID) {
        //humanVerified.add(playerUUID);
    }

    public void removeHumanVerified(ProxiedPlayer proxiedPlayer) {
        removeHumanVerified(proxiedPlayer.getUniqueId());
    }

    public void removeHumanVerified(UUID playerUUID) {
        //humanVerified.remove(playerUUID);
    }

}
