package com.minecraftly.modules.homeworlds.data;

import com.minecraftly.core.bukkit.config.ConfigWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.UUID;

/**
 * Created by Keir on 30/04/2015.
 */
public class PlayerGlobalData implements PlayerData {

    private final UUID uuid;
    protected final ConfigWrapper globalPlayerData;

    private final ItemStack[] inventoryContents = new ItemStack[InventoryType.PLAYER.getDefaultSize()];
    private final ItemStack[] enderChestContents = new ItemStack[InventoryType.ENDER_CHEST.getDefaultSize()];

    protected PlayerGlobalData(UUID uuid, File globalPlayerDataFile) {
        this.uuid = uuid;
        this.globalPlayerData = new ConfigWrapper(globalPlayerDataFile);

        if (globalPlayerDataFile.exists()) {
            loadFromFile();
        } else {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) {
                throw new UnsupportedOperationException("Attempted to load player global data for first time whilst player is offline.");
            }

            copyFromPlayer(player);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public ItemStack[] getEnderChestContents() {
        return enderChestContents;
    }

    @Override
    public void loadFromFile() {
        globalPlayerData.reloadConfig();
        FileConfiguration configuration = globalPlayerData.getConfig();
        ConfigurationSection inventorySection = configuration.contains("inventory") ? configuration.getConfigurationSection("inventory") : null;
        ConfigurationSection enderChestSection = configuration.contains("enderChest") ? configuration.getConfigurationSection("enderChest") : null;

        if (inventorySection != null) {
            for (String slotString : inventorySection.getKeys(false)) {
                int slot = Integer.parseInt(slotString);
                inventoryContents[slot] = inventorySection.getItemStack(slotString);
            }
        }

        if (enderChestSection != null) {
            for (String slotString : enderChestSection.getKeys(false)) {
                int slot = Integer.parseInt(slotString);
                enderChestContents[slot] = enderChestSection.getItemStack(slotString);
            }
        }
    }

    @Override
    public void saveToFile() {
        FileConfiguration configuration = globalPlayerData.getConfig();

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            ItemStack itemStack = inventoryContents[slot];

            if (itemStack != null) {
                configuration.set("inventory." + String.valueOf(slot), itemStack);
            }
        }

        for (int slot = 0; slot < enderChestContents.length; slot++) {
            ItemStack itemStack = enderChestContents[slot];

            if (itemStack != null) {
                configuration.set("enderChest." + String.valueOf(slot), itemStack);
            }
        }

        globalPlayerData.saveConfig();
    }

    @Override
    public void copyToPlayer(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        Inventory enderInventory = player.getEnderChest();

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            playerInventory.setItem(slot, inventoryContents[slot]);
        }

        for (int slot = 0; slot < enderChestContents.length; slot++) {
            enderInventory.setItem(slot, enderChestContents[slot]);
        }
    }

    @Override
    public void copyFromPlayer(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        Inventory enderInventory = player.getEnderChest();

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            inventoryContents[slot] = playerInventory.getItem(slot);
        }

        for (int slot = 0; slot < enderChestContents.length; slot++) {
            enderChestContents[slot] = enderInventory.getItem(slot);
        }
    }

}
