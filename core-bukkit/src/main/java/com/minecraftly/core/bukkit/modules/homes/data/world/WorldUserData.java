package com.minecraftly.core.bukkit.modules.homes.data.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.database.YamlConfigurationResultHandler;
import com.minecraftly.core.bukkit.modules.homes.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.homes.WorldDimension;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.ResettableData;
import com.minecraftly.core.bukkit.user.modularisation.UserData;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.Achievement;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Created by Keir on 09/06/2015.
 */
public class WorldUserData extends UserData implements ResettableData {

    private final UUID worldUUID;
    private YamlConfiguration yamlConfiguration = new YamlConfiguration();

    public WorldUserData(UUID worldUUID, User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
        checkNotNull(worldUUID);
        this.worldUUID = worldUUID;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    public Location getLastLocation() {
        return yamlConfiguration.isConfigurationSection("lastLocation")
                ? checkLocation(BukkitUtilities.getLocation(yamlConfiguration.getConfigurationSection("lastLocation").getValues(true)))
                : null;
    }

    public Location getBedLocation() {
        return yamlConfiguration.isConfigurationSection("bedLocation")
                ? checkLocation(BukkitUtilities.getLocation(yamlConfiguration.getConfigurationSection("bedLocation").getValues(true)))
                : null;
    }

    public int getRemainingAir() {
        return yamlConfiguration.getInt("air", 300);
    }

    public int getFireTicks() {
        return yamlConfiguration.getInt("fire", 0);
    }

    public int getFoodLevel() {
        return yamlConfiguration.getInt("food", 20);
    }

    public int getTotalExperience() {
        return yamlConfiguration.getInt("experience", 0);
    }

    public float getExhaustion() {
        return yamlConfiguration.getInt("exhaustion", 0);
    }

    public float getSaturation() {
        return yamlConfiguration.getInt("saturation", 20);
    }

    public float getFallDistance() {
        return yamlConfiguration.getInt("fallDistance", 0);
    }

    public List<String> getAchievements() {
        return yamlConfiguration.getStringList("achievements");
    }

    @Override
    public void extractFrom(Player player) {
        Location lastLocation = checkLocation(player.getLocation());
        yamlConfiguration.set("lastLocation", lastLocation != null ? BukkitUtilities.getLocationContainer(player.getLocation()).serialize() : null);

        Location bedLocation = checkLocation(player.getBedSpawnLocation());
        yamlConfiguration.set("bedLocation", bedLocation != null ? BukkitUtilities.getLocationContainer(bedLocation).serialize() : null);

        yamlConfiguration.set("air", player.getRemainingAir());
        yamlConfiguration.set("fire", player.getFireTicks());
        yamlConfiguration.set("food", player.getFoodLevel());
        yamlConfiguration.set("experience", player.getTotalExperience());

        yamlConfiguration.set("exhaustion", player.getExhaustion());
        yamlConfiguration.set("saturation", player.getSaturation());
        yamlConfiguration.set("fallDistance", player.getFallDistance());

        List<String> achievements = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (player.hasAchievement(achievement)) {
                achievements.add(achievement.name());
            }
        }

        yamlConfiguration.set("achievements", achievements);
    }

    private Location checkLocation(Location location) {
        if (location != null) {
            World world = location.getWorld();

            if (world != null) {
                UUID worldOwner = ModulePlayerWorlds.getInstance().getHomeOwner(WorldDimension.getBaseWorld(world));

                if (worldOwner == null || !worldOwner.equals(getUser().getUniqueId())) {
                    location = null;
                }
            } else {
                location = null;
            }
        }

        return location;
    }

    @Override
    protected void initialLoad() {
        super.initialLoad();
        yamlConfiguration.set("lastLocation", null); // this will be set to the wrong world on initial load
    }

    @Override
    public void apply(Player player) {
        player.setBedSpawnLocation(yamlConfiguration.isConfigurationSection("bedLocation")
                ? BukkitUtilities.getLocation(yamlConfiguration.getConfigurationSection("bedLocation")) : null);

        player.setRemainingAir(getRemainingAir());
        player.setFireTicks(getFireTicks());
        player.setFoodLevel(getFoodLevel());
        player.setTotalExperience(getTotalExperience());

        player.setExhaustion(getExhaustion());
        player.setSaturation(getSaturation());
        player.setFallDistance(getFallDistance());

        List<String> achievements = getAchievements();
        for (Achievement achievement : Achievement.values()) {
            if (achievements.contains(achievement.name())) {
                player.awardAchievement(achievement);
            } else {
                player.removeAchievement(achievement);
            }
        }
    }

    @Override
    public void load() throws SQLException {
        super.load();

        YamlConfiguration yamlConfiguration = getQueryRunnerSupplier().get().query(
                String.format(
                        "SELECT `data` FROM `%sworld_user_data` WHERE `world_uuid` = UNHEX(?) AND `user_uuid` = UNHEX(?)",
                        DatabaseManager.TABLE_PREFIX
                ),
                YamlConfigurationResultHandler.DATA_FIELD_INSTANCE,
                Utilities.convertToNoDashes(worldUUID),
                Utilities.convertToNoDashes(getUser().getUniqueId())
        );

        if (yamlConfiguration == null) {
            initialLoad();
        } else {
            this.yamlConfiguration = yamlConfiguration;
        }
    }

    @Override
    public void save() throws SQLException {
        super.save();

        getQueryRunnerSupplier().get().update(String.format("REPLACE INTO `%sworld_user_data` (`world_uuid`, `user_uuid`, `data`) VALUES (UNHEX(?), UNHEX(?), ?)",
                        DatabaseManager.TABLE_PREFIX),
                Utilities.convertToNoDashes(worldUUID),
                Utilities.convertToNoDashes(getUser().getUniqueId()),
                yamlConfiguration.saveToString());
    }

    @Override
    public void reset() {
        yamlConfiguration = new YamlConfiguration();

        Player player = getUser().getPlayer();
        if (player != null) {
            apply(player);
        } else {
            try {
                save();
            } catch (SQLException e) {
                e.printStackTrace(); // todo
            }
        }
    }
}
