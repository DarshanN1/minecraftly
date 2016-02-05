package com.minecraftly.bukkit.modules.fun;

import com.minecraftly.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.bukkit.modules.Module;
import com.sk89q.intake.fluent.DispatcherNode;

/**
 * Created by Keir on 14/07/2015.
 */
public class ModuleFun extends Module {

    public ModuleFun(MclyCoreBukkitPlugin plugin) {
        super("Fun", plugin);
    }

    @Override
    public void registerCommands(DispatcherNode dispatcherNode) {
        dispatcherNode.registerMethods(new CommandHat(this, getPlugin().getLanguageManager()));
    }
}
