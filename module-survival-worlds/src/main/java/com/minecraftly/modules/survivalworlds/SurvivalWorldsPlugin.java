package com.minecraftly.modules.survivalworlds;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Keir on 23/04/2015.
 */
public class SurvivalWorldsPlugin extends Module {

    private MinecraftlyCore plugin;
    private final Map<UUID, World> playerWorlds = new HashMap<>();

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), plugin);
    }

    @Override
    protected void onDisable(MinecraftlyCore plugin) {
        this.plugin = null;
    }

    public World getWorld(UUID uuid) {
        World world = playerWorlds.get(uuid);

        if (world == null) {
            String uuidString = uuid.toString();
            world = Bukkit.getWorld(uuid.toString());

            if (world == null) {
                WorldCreator worldCreator = new WorldCreator(uuidString);
                File worldDirectory = new File(Bukkit.getWorldContainer(), uuidString);

                if (worldDirectory.exists() && worldDirectory.isDirectory()) {
                    world = worldCreator.createWorld(); // this actually loads an existing world
                } else { // generate world async
                    // todo start generation from BungeeCord?
                    return worldCreator.createWorld();
                }
            }
        }

        return world;
    }

}
