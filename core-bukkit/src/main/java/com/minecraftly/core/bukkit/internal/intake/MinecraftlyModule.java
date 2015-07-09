package com.minecraftly.core.bukkit.internal.intake;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.sk89q.intake.parametric.AbstractModule;
import lc.vq.exhaust.command.annotation.Sender;

import java.util.logging.Logger;

/**
 * Created by Keir on 08/07/2015.
 */
public class MinecraftlyModule extends AbstractModule {

    private final MclyCoreBukkitPlugin plugin;

    public MinecraftlyModule(MclyCoreBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(MinecraftlyCore.class).toInstance(plugin);
        bind(MclyCoreBukkitPlugin.class).toInstance(plugin);
        bind(UserManager.class).toInstance(plugin.getUserManager());
        bind(LanguageManager.class).toInstance(plugin.getLanguageManager());
        bind(DatabaseManager.class).toInstance(plugin.getDatabaseManager());
        bind(Logger.class).toInstance(plugin.getLogger());
        bind(User.class).annotatedWith(Sender.class).toProvider(SenderUserProvider.INSTANCE);
        bind(User.class).toProvider(UserProvider.INSTANCE);
    }

}
