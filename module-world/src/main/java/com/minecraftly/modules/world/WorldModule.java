package com.minecraftly.modules.world;

import com.google.common.collect.ImmutableList;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.ConfigManager;
import com.sk89q.intake.Parameter;
import com.sk89q.intake.SettableDescription;
import com.sk89q.intake.SettableParameter;
import com.sk89q.intake.fluent.DispatcherNode;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Keir on 18/03/2015.
 */
public class WorldModule extends Module {

    private ConfigManager configManager;
    private List<String> loadedWorlds = new ArrayList<>();

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        configManager = new ConfigManager(new File(plugin.getGeneralDataDirectory(), "loaded-worlds.yml"));
        loadedWorlds = configManager.getConfig().getStringList("loadedWorlds");
        loadWorlds();
    }

    @Override
    protected void onDisable(MinecraftlyCore plugin) {
        configManager.getConfig().set("loadedWorlds", loadedWorlds);
        configManager.saveConfig();
    }

    @Override
    protected void registerCommands(DispatcherNode dispatcherNode) {
        SettableDescription description = (SettableDescription) dispatcherNode.group("world")
                .describeAs("Commands for managing worlds.")
                .registerMethods(new WorldCommands(this))
                .getDispatcher().getDescription();

        description.setParameters(ImmutableList.<Parameter>builder()
                .add(new SettableParameter("load/unload"))
                .build());
    }

    public void loadWorlds() {
        if (loadedWorlds.size() > 0) {
            getLogger().info("Loading worlds: " + StringUtils.join(loadedWorlds, ", "));

            for (String worldName : loadedWorlds) {
                World world = Bukkit.getWorld(worldName);

                if (world == null) { // if not already loaded
                    loadWorld(worldName);
                }
            }
        }
    }

    public World loadWorld(String worldName) {
        World world = Bukkit.createWorld(new WorldCreator(worldName));
        return world;
    }

    public void addStartupLoadTask(World world) {
        addStartupLoadTask(world.getName());
    }

    public void addStartupLoadTask(String worldName) {
        loadedWorlds.add(worldName);
    }

    public void removeStartupLoadTask(World world) {
        removeStartupLoadTask(world.getName());
    }

    public void removeStartupLoadTask(String worldName) {
        loadedWorlds.remove(worldName);
    }

}
