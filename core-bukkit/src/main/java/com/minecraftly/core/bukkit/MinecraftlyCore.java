package com.minecraftly.core.bukkit;

import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.bukkit.config.ConfigManager;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.user.UserManager;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.function.Supplier;

/**
 * Created by Keir on 20/03/2015.
 */
public interface MinecraftlyCore extends Plugin {

    ConfigManager getConfigManager();

    DatabaseManager getDatabaseManager();

    LanguageManager getLanguageManager();

    UserManager getUserManager();

    ServerGateway<Player> getGateway();

    File getGeneralDataDirectory();

    File getBackupsDirectory();

    PlayerSwitchJobManager getPlayerSwitchJobManager();

    Supplier<QueryRunner> getQueryRunnerSupplier();
}
