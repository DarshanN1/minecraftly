package com.minecraftly.modules.survivalworlds.data;

import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

/**
 * Created by Keir on 30/04/2015.
 */
public class PlayerWorldData implements PlayerData {

    private final UUID uuid;
    private final ConfigManager worldPlayerData;

    private Location lastLocation;
    private Location bedLocation;

    private int air;
    private int fire;
    private int food;
    private int experience;

    private float exhaustion;
    private float saturation;
    private float fallDistance;

    private GameMode gameMode;

    protected PlayerWorldData(UUID uuid, File worldPlayerDataFile) {
        this.uuid = uuid;
        this.worldPlayerData = new ConfigManager(worldPlayerDataFile);

        System.out.println("New player world data " + uuid);

        if (worldPlayerDataFile.exists()) {
            loadFromFile();
        } else {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) {
                throw new UnsupportedOperationException("Attempted to load player world data for first time whilst player is offline.");
            }

            copyFromPlayer(player);
            lastLocation = null; // "un-copy" this, causes invalid location on first run
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public Location getBedLocation() {
        return bedLocation;
    }

    public int getAir() {
        return air;
    }

    public int getFire() {
        return fire;
    }

    public int getFood() {
        return food;
    }

    public int getExperience() {
        return experience;
    }

    public float getExhaustion() {
        return exhaustion;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getFallDistance() {
        return fallDistance;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    @Override
    public void loadFromFile() {
        worldPlayerData.reloadConfig();
        FileConfiguration configuration = worldPlayerData.getConfig();

        lastLocation = BukkitUtilities.getLocation(configuration.getConfigurationSection("lastLocation"));
        bedLocation = configuration.contains("bedLocation") ? BukkitUtilities.getLocation(configuration.getConfigurationSection("bedLocation")) : null;

        air = configuration.getInt("air");
        fire = configuration.getInt("fire");
        food = configuration.getInt("food");
        experience = configuration.getInt("experience");

        exhaustion = configuration.getInt("exhaustion");
        saturation = configuration.getInt("saturation");
        fallDistance = configuration.getInt("fallDistance");

        gameMode = GameMode.valueOf(configuration.getString("gameMode"));
    }

    @Override
    public void saveToFile() {
        FileConfiguration configuration = worldPlayerData.getConfig();

        configuration.set("lastLocation", BukkitUtilities.getLocationContainer(lastLocation).serialize());
        configuration.set("bedLocation", bedLocation != null ? BukkitUtilities.getLocationContainer(bedLocation).serialize() : null);

        configuration.set("air", air);
        configuration.set("fire", fire);
        configuration.set("food", food);
        configuration.set("experience", experience);

        configuration.set("exhaustion", exhaustion);
        configuration.set("saturation", saturation);
        configuration.set("fallDistance", fallDistance);

        configuration.set("gameMode", gameMode.name());

        worldPlayerData.saveConfig();
        System.out.println("Saved to file");
    }

    @Override
    public void copyToPlayer(Player player) {
        player.setBedSpawnLocation(bedLocation);

        player.setRemainingAir(air);
        player.setFireTicks(fire);
        player.setFoodLevel(food);
        player.setTotalExperience(experience);

        player.setExhaustion(exhaustion);
        player.setSaturation(saturation);
        player.setFallDistance(fallDistance);

        player.setGameMode(gameMode);
    }

    @Override
    public void copyFromPlayer(Player player) {
        lastLocation = player.getLocation();
        bedLocation = player.getBedSpawnLocation();

        air = player.getRemainingAir();
        fire = player.getFireTicks();
        food = player.getFoodLevel();
        experience = player.getTotalExperience();

        exhaustion = player.getExhaustion();
        saturation = player.getSaturation();
        fallDistance = player.getFallDistance();

        gameMode = player.getGameMode();
    }

}
