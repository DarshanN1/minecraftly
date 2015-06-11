package com.minecraftly.modules.homeworlds.data.global;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Keir on 08/06/2015.
 */
public class GlobalUserData extends SingletonUserData implements ResultSetHandler<YamlConfiguration>, Consumer<Player> {

    private YamlConfiguration yamlConfiguration = new YamlConfiguration();

    public GlobalUserData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

    @Override
    public void extractFrom(Player player) {
        yamlConfiguration = new YamlConfiguration();

        PlayerInventory playerInventory = player.getInventory();
        ConfigurationSection playerInventorySection = yamlConfiguration.isConfigurationSection("playerInventory")
                ? yamlConfiguration.getConfigurationSection("playerInventory")
                : yamlConfiguration.createSection("playerInventory");

        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack itemStack = playerInventory.getItem(i);
            playerInventorySection.set(String.valueOf(i), itemStack);
        }

        Inventory enderInventory = player.getEnderChest();
        ConfigurationSection enderInventorySection = yamlConfiguration.isConfigurationSection("enderInventory")
                ? yamlConfiguration.getConfigurationSection("enderInventory")
                : yamlConfiguration.createSection("enderInventory");

        for (int i = 0; i < enderInventory.getSize(); i++) {
            ItemStack itemStack = enderInventory.getItem(i);

            if (itemStack != null) {
                enderInventorySection.set(String.valueOf(i), itemStack);
            }
        }
    }

    @Override
    public void load() throws SQLException {
        super.load();

        YamlConfiguration yamlConfiguration = getQueryRunnerSupplier().get().query(
                String.format("SELECT `data` FROM %sglobal_user_data WHERE `uuid` = UNHEX(?)", DatabaseManager.TABLE_PREFIX),
                this,
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
    public void apply(Player player) {
        if (yamlConfiguration.contains("playerInventory")) {
            PlayerInventory playerInventory = player.getInventory();
            ConfigurationSection playerInventorySection = yamlConfiguration.getConfigurationSection("playerInventory");
            for (int i = 0; i < playerInventory.getSize(); i++) {
                String intString = String.valueOf(i);
                ItemStack itemStack = playerInventorySection.contains(intString) ? playerInventorySection.getItemStack(intString) : null;
                playerInventory.setItem(i, itemStack);
            }
        }

        if (yamlConfiguration.contains("enderInventory")) {
            Inventory enderInventory = player.getEnderChest();
            ConfigurationSection enderInventorySection = yamlConfiguration.getConfigurationSection("playerInventory");
            for (int i = 0; i < enderInventory.getSize(); i++) {
                String intString = String.valueOf(i);
                ItemStack itemStack = enderInventorySection.contains(intString) ? enderInventorySection.getItemStack(intString) : null;
                enderInventory.setItem(i, itemStack);
            }
        }
    }

    @Override
    public void save() throws SQLException {
        super.save();

        getQueryRunnerSupplier().get().update(
                String.format("REPLACE INTO `%sglobal_user_data`(`uuid`, `data`) VALUES(UNHEX(?), ?)",
                        DatabaseManager.TABLE_PREFIX),
                Utilities.convertToNoDashes(getUser().getUniqueId()),
                yamlConfiguration.saveToString()
        );
    }

    // executed when player is about to switch server
    @Override
    public void accept(Player player) {
        extractFrom(player);

        try {
            save();
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error saving your data: " + e.getMessage());
            throw new RuntimeException("Unable to save data for '" + player.getUniqueId() + "'.", e);
        }
    }
}
