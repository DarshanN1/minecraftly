package com.minecraftly.testmodule;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.module.Module;
import com.sk89q.intake.fluent.DispatcherNode;

/**
 * Created by Keir on 12/03/2015.
 */
public class TestModule extends Module {

    @Override
    protected void onEnable(MinecraftlyCore plugin) {
        getLogger().info("This is a test message printed by a Minecraftly module.");
    }

    @Override
    protected void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new TestModuleCommand());
    }

}
