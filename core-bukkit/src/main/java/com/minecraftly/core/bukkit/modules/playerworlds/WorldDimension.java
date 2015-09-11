package com.minecraftly.core.bukkit.modules.playerworlds;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper class for converting to and from different dimensions (worlds).
 */
public enum WorldDimension {

    NETHER(World.Environment.NETHER, "The Nether", "_nether"), THE_END(World.Environment.THE_END, "The End", "_the_end");

    private World.Environment environment;
    private String niceName;
    private String suffix;

    WorldDimension(World.Environment environment, String niceName, String suffix) {
        this.environment = environment;
        this.niceName = niceName;
        this.suffix = suffix;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public String getNiceName() {
        return niceName;
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
        checkNotNull(world);
        return Bukkit.getWorld(convertTo(world.getName()));
    }

    public void convertToLoad(World world, Consumer<World> consumer) {
        checkNotNull(world);
        checkNotNull(consumer);

        World loadedWorld = convertTo(world);

        if (loadedWorld != null) {
            consumer.accept(loadedWorld);
        } else {
            String newWorldName = convertTo(world.getName());
            ModulePlayerWorlds.getInstance().loadWorld(newWorldName, getEnvironment(), consumer);
        }
    }

    public static WorldDimension fromEnvironment(World.Environment environment) {
        for (WorldDimension worldDimension : values()) {
            if (worldDimension.getEnvironment() == environment) {
                return worldDimension;
            }
        }

        return null;
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
