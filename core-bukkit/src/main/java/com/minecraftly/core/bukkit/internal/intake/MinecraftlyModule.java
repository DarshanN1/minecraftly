package com.minecraftly.core.bukkit.internal.intake;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;
import com.sk89q.intake.parametric.AbstractModule;
import lc.vq.exhaust.command.annotation.Sender;

/**
 * Created by Keir on 08/07/2015.
 */
public class MinecraftlyModule extends AbstractModule {

    private final MclyCoreBukkitPlugin plugin;
    private final UserManager userManager;
    private final LanguageManager languageManager;
    private final DatabaseManager databaseManager;

    public MinecraftlyModule(MclyCoreBukkitPlugin plugin, UserManager userManager, LanguageManager languageManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.languageManager = languageManager;
        this.databaseManager = databaseManager;
    }

    @Override
    protected void configure() {
        bind(MinecraftlyCore.class).toInstance(plugin);
        bind(MclyCoreBukkitPlugin.class).toInstance(plugin);
        bind(UserManager.class).toInstance(userManager);
        bind(LanguageManager.class).toInstance(languageManager);
        bind(DatabaseManager.class).toInstance(databaseManager);
        bind(User.class).annotatedWith(Sender.class).toProvider(SenderUserProvider.INSTANCE);
        bind(User.class).toProvider(UserProvider.INSTANCE);
    }

}
