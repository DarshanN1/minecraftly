package com.minecraftly.core.bukkit.modules.playerworlds.data.global;

import com.minecraftly.core.utilities.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.database.YamlConfigurationResultHandler;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Keir on 08/06/2015.
 */
public class GlobalUserData extends SingletonUserData implements Consumer<Player> {

    private YamlConfiguration yamlConfiguration = new YamlConfiguration();

    public GlobalUserData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

    @Override
    public void extractFrom(Player player) {
        yamlConfiguration = new YamlConfiguration();

        PlayerInventory playerInventory = player.getInventory();
        ConfigurationSection playerInventorySection = BukkitUtilities.getOrCreateSection(yamlConfiguration, "playerInventory");

        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack itemStack = playerInventory.getItem(i);
            playerInventorySection.set(String.valueOf(i), itemStack);
        }

        Inventory enderInventory = player.getEnderChest();
        ConfigurationSection enderInventorySection = BukkitUtilities.getOrCreateSection(yamlConfiguration, "enderInventory");

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
                String.format("SELECT `extra_data` FROM %sglobal_user_data WHERE `uuid` = UNHEX(?)", DatabaseManager.TABLE_PREFIX),
                YamlConfigurationResultHandler.EXTRA_DATA_FIELD_HANDLER_INSTANCE,
                Utilities.convertToNoDashes(getUser().getUniqueId())
        );

        if (yamlConfiguration == null) {
            initialLoad();
        } else {
            this.yamlConfiguration = yamlConfiguration;
        }
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
                String.format("REPLACE INTO `%sglobal_user_data`(`uuid`, `extra_data`) VALUES(UNHEX(?), ?)",
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
