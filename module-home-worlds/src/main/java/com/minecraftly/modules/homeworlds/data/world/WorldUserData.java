package com.minecraftly.modules.homeworlds.data.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.modules.homeworlds.HomeWorldsModule;
import com.minecraftly.modules.homeworlds.WorldDimension;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Created by Keir on 09/06/2015.
 */
public class WorldUserData extends SingletonUserData implements ResultSetHandler<YamlConfiguration> {

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
                ? BukkitUtilities.getLocation(yamlConfiguration.getConfigurationSection("lastLocation").getValues(true))
                : null;
    }

    public Location getBedLocation() {
        return yamlConfiguration.isConfigurationSection("bedLocation")
                ? BukkitUtilities.getLocation(yamlConfiguration.getConfigurationSection("bedLocation").getValues(true))
                : null;
    }

    public int getAir() {
        return yamlConfiguration.getInt("air");
    }

    public int getFire() {
        return yamlConfiguration.getInt("fire");
    }

    public int getFood() {
        return yamlConfiguration.getInt("food");
    }

    public int getExperience() {
        return yamlConfiguration.getInt("experience");
    }

    public float getExhaustion() {
        return yamlConfiguration.getInt("exhaustion");
    }

    public float getSaturation() {
        return yamlConfiguration.getInt("saturation");
    }

    public float getFallDistance() {
        return yamlConfiguration.getInt("fallDistance");
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
    }

    private Location checkLocation(Location location) {
        if (location != null) {
            World world = location.getWorld();

            if (world != null) {
                UUID worldOwner = HomeWorldsModule.getInstance().getHomeOwner(WorldDimension.getBaseWorld(world));

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

        player.setRemainingAir(yamlConfiguration.getInt("air"));
        player.setFireTicks(yamlConfiguration.getInt("fire"));
        player.setFoodLevel(yamlConfiguration.getInt("food"));
        player.setTotalExperience(yamlConfiguration.getInt("experience"));

        player.setExhaustion(yamlConfiguration.getInt("exhaustion"));
        player.setSaturation(yamlConfiguration.getInt("saturation"));
        player.setFallDistance(yamlConfiguration.getInt("fallDistance"));
    }

    @Override
    public void load() throws SQLException {
        super.load();

        YamlConfiguration yamlConfiguration = getQueryRunnerSupplier().get().query(
                String.format("SELECT `data` FROM `%sworld_user_data` WHERE `world_uuid` = UNHEX(?) AND `user_uuid` = UNHEX(?)",
                        DatabaseManager.TABLE_PREFIX
                ),
                this,
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
    public YamlConfiguration handle(ResultSet rs) throws SQLException {
        if (rs.next()) {
            try {
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.loadFromString(rs.getString("data"));
                return yamlConfiguration;
            } catch (InvalidConfigurationException e) {
                throw new RuntimeException("Unable to parse yml from database.", e); // todo exception type
            }
        }

        return null;
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

}
