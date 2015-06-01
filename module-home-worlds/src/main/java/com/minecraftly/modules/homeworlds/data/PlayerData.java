package com.minecraftly.modules.homeworlds.data;

import org.bukkit.entity.Player;

/**
 * Created by Keir on 03/05/2015.
 */
public interface PlayerData {
    void loadFromFile();
    void saveToFile();
    void copyToPlayer(Player player);
    void copyFromPlayer(Player player);
}
