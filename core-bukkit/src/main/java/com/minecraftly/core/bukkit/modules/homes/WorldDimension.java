package com.minecraftly.core.bukkit.modules.homes;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for converting to and from different dimensions (worlds).
 */
public enum WorldDimension {

    NETHER(World.Environment.NETHER, "_nether"), THE_END(World.Environment.THE_END, "_the_end");

    private World.Environment environment;
    private String suffix;

    WorldDimension(World.Environment environment, String suffix) {
        this.environment = environment;
        this.suffix = suffix;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean matches(String worldName) {
        checkNotNull(worldName);
        return worldName.endsWith(suffix);
    }

    public String getBaseName(String worldName) {
        checkNotNull(worldName);

        for (WorldDimension worldDimension : values()) {
            if (worldDimension.matches(worldName)) {
                return worldName.substring(0, worldName.length() - suffix.length());
            }
        }

        return worldName;
    }

    public String convertTo(String worldName) {
        checkNotNull(worldName);
        return getBaseName(worldName) + suffix;
    }

    public World convertTo(World world) {
        return convertTo(world, false);
    }

    public World convertTo(World world, boolean load) {
        checkNotNull(world);
        String newWorldName = convertTo(world.getName());
        World newWorld = Bukkit.getWorld(newWorldName);

        if (newWorld == null && load) {
            WorldCreator worldCreator = new WorldCreator(newWorldName);
            worldCreator.environment(getEnvironment());
            newWorld = Bukkit.createWorld(worldCreator); // todo generate elsewhere?
        }

        return newWorld;
    }

    public static String getBaseWorldName(String worldName) {
        checkNotNull(worldName);
        String returnWorldName = worldName;

        for (WorldDimension worldDimension : values()) {
            if (worldDimension.matches(worldName)) {
                returnWorldName = worldDimension.getBaseName(worldName);
                break;
            }
        }

        return returnWorldName;
    }

    public static World getBaseWorld(World world) {
        checkNotNull(world);
        World baseWorld = Bukkit.getWorld(getBaseWorldName(world.getName()));
        return baseWorld != null ? baseWorld : world;
    }

    public static List<Player> getPlayersAllDimensions(World baseWorld) {
        checkNotNull(baseWorld);
        List<Player> players = new ArrayList<>(baseWorld.getPlayers());

        for (WorldDimension worldDimension : WorldDimension.values()) {
            World world = worldDimension.convertTo(baseWorld);
            if (world != null) {
                players.addAll(world.getPlayers());
            }
        }

        return players;
    }

}
