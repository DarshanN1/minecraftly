package com.minecraftly.core.bukkit.user;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.SoftReference;
import java.util.UUID;

/**
 * Created by Keir on 13/03/2015.
 */
public class User {

    private final UUID uuid;
    private SoftReference<Player> playerSoftReference;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        if (playerSoftReference == null || playerSoftReference.get() == null) {
            Player player = Bukkit.getPlayer(getUniqueId());

            if (player != null) {
                this.playerSoftReference = new SoftReference<>(player);
            }
        }

        return playerSoftReference.get();
    }

    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }
}
