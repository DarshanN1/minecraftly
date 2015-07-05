package com.minecraftly.core.bukkit.modules.chest;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.config.DataValue;
import com.minecraftly.core.bukkit.modules.Module;
import com.minecraftly.core.bukkit.modules.chest.data.ChestStorageHandler;
import com.minecraftly.core.bukkit.modules.chest.data.LegacyFormatConverter;
import com.sk89q.intake.fluent.DispatcherNode;

import java.io.File;

/**
 * Created by Keir on 05/07/2015.
 */
public class ModuleChest extends Module {

    private final DataValue<Integer> maximumChests = new DataValue<>(50, Integer.class);

    public ModuleChest(MclyCoreBukkitPlugin plugin) {
        super("Chest", plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().getConfigManager().register("chests.maximumChests", maximumChests);
        getPlugin().getUserManager().addDataStorageHandler(new ChestStorageHandler(getPlugin().getQueryRunnerSupplier()));

        File conversionFolder = new File(getPlugin().getDataFolder(), "chest-conversion");
        if (conversionFolder.exists() && conversionFolder.isDirectory()) {
            LegacyFormatConverter legacyFormatConverter = new LegacyFormatConverter(getPlugin().getUserManager(), conversionFolder, getLogger());
            legacyFormatConverter.convert();
            getLogger().info("Conversion finished, renaming conversion folder.");

            if (!conversionFolder.renameTo(new File(conversionFolder.getParent(), "chest-conversion-finished"))) {
                getLogger().severe("Unable to rename conversion folder, this may cause an overwrite of data on the next startup.");
            }
        }
    }

    @Override
    public void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new ChestCommand(this, getPlugin().getUserManager()));
    }

    public DataValue<Integer> getMaximumChests() {
        return maximumChests;
    }
}
