package com.minecraftly.core.bukkit.modules.chest.data;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.database.YamlConfigurationResultHandler;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by Keir on 05/07/2015.
 */
public class UserChestData extends SingletonUserData {

    private static final int DEFAULT_ROWS = 6;

    private Map<Integer, Inventory> inventories = new HashMap<>();

    public UserChestData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

    public Map<Integer, Inventory> getInventories() {
        return inventories;
    }

    public boolean hasInventory(int number) {
        return inventories.containsKey(number);
    }

    public Inventory getInventory(int number, boolean createIfNotExists) {
        Inventory inventory = inventories.get(number);

        if (inventory == null && createIfNotExists) {
            inventory = Bukkit.createInventory(getUser().getPlayer(), DEFAULT_ROWS * 9, getInventoryName(number));
            inventories.put(number, inventory);
        }

        return inventory;
    }

    public List<Exception> parse(ConfigurationSection configurationSection) {
        List<Exception> exceptions = new ArrayList<>();

        try {
            for (String chestNumberString : configurationSection.getKeys(false)) {
                int chestNumber;

                try {
                    chestNumber = Integer.parseInt(chestNumberString);
                } catch (NumberFormatException e) {
                    exceptions.add(new Exception("Chest number is invalid.", e));
                    continue;
                }

                ConfigurationSection chestSection = configurationSection.getConfigurationSection(chestNumberString);
                ConfigurationSection itemsSection = chestSection.getConfigurationSection("items");
                Inventory inventory = Bukkit.createInventory(getUser().getPlayer(), chestSection.getInt("rows") * 9, getInventoryName(chestNumber));

                for (String slotString : itemsSection.getKeys(false)) {
                    int slot;

                    try {
                        slot = Integer.parseInt(slotString);
                    } catch (NumberFormatException e) {
                        exceptions.add(new Exception("Slot number is invalid for chest number: " + chestNumber, e));
                        continue;
                    }

                    inventory.setItem(slot, itemsSection.getItemStack(slotString));
                }

                inventories.put(chestNumber, inventory);
            }
        } catch (Exception e) {
            exceptions.add(e);
        }

        return exceptions;
    }

    @Override
    public void load() throws SQLException {
        super.load();

        YamlConfiguration yamlConfiguration = getQueryRunnerSupplier().get().query(
                String.format(
                        "SELECT `data` FROM `%suser_chests` WHERE `uuid` = UNHEX(?)",
                        DatabaseManager.TABLE_PREFIX
                ),
                YamlConfigurationResultHandler.DATA_FIELD_INSTANCE,
                Utilities.convertToNoDashes(getUser().getUniqueId())
        );

        if (yamlConfiguration != null) {
            List<Exception> exceptions = parse(yamlConfiguration);

            // todo sout's are horrible
            if (exceptions.size() > 0) {
                System.out.println("There were some error(s) whilst loading user chest data.");

                for (Exception exception : exceptions) {
                    exception.printStackTrace();
                    System.out.println("#####################################################");
                }
            }
        }
    }

    @Override
    public void save() throws SQLException {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        for (Map.Entry<Integer, Inventory> entry : inventories.entrySet()) {
            int chestNumber = entry.getKey();
            Inventory inventory = entry.getValue();

            ConfigurationSection chestSection = yamlConfiguration.createSection(String.valueOf(chestNumber));
            ConfigurationSection itemsSection = chestSection.createSection("items");

            chestSection.set("rows", inventory.getSize() / 9);

            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack itemStack = inventory.getItem(slot);

                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    itemsSection.set(String.valueOf(slot), itemStack);
                }
            }
        }

        getQueryRunnerSupplier().get().update(
                String.format(
                        "REPLACE INTO `%suser_chests` (`uuid`, `data`) VALUES (UNHEX(?), ?)",
                        DatabaseManager.TABLE_PREFIX
                ),
                Utilities.convertToNoDashes(getUser().getUniqueId()),
                yamlConfiguration.saveToString()
        );
    }

    private String getInventoryName(int number) {
        return "Chest #" + (number + 1);
    }
}
