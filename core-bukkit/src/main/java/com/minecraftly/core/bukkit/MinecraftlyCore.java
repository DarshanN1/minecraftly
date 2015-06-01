package com.minecraftly.core.bukkit;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.ContentOwner;
import com.minecraftly.core.bukkit.config.ConfigManager;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.module.ModuleManager;
import com.minecraftly.core.bukkit.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Created by Keir on 20/03/2015.
 */
public interface MinecraftlyCore extends Plugin, ContentOwner {

    ConfigManager getConfigManager();

    DatabaseManager getDatabaseManager();

    LanguageManager getLanguageManager();

    UserManager getUserManager();

    ModuleManager getModuleManager();

    ServerGateway<Player> getGateway();

    File getGeneralDataDirectory();

    File getBackupsDirectory();

    PlayerQuitJobManager getPlayerQuitJobManager();
}
