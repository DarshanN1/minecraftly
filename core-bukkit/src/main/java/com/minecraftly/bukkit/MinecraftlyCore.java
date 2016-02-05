package com.minecraftly.bukkit;

import com.google.gson.Gson;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.bukkit.config.ConfigManager;
import com.minecraftly.bukkit.database.DatabaseManager;
import com.minecraftly.bukkit.language.LanguageManager;
import com.minecraftly.bukkit.user.UserManager;
import com.minecraftly.bukkit.redis.JedisService;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * Created by Keir on 20/03/2015.
 */
public interface MinecraftlyCore extends Plugin {

    String getComputeUniqueId();

    InetSocketAddress getInstanceExternalSocketAddress();

    JedisService getJedisService();

    Gson getGson();

    ConfigManager getConfigManager();

    DatabaseManager getDatabaseManager();

    LanguageManager getLanguageManager();

    UserManager getUserManager();

    ServerGateway<Player> getGateway();

    File getGeneralDataDirectory();

    File getBackupsDirectory();

    PlayerSwitchJobManager getPlayerSwitchJobManager();

    Supplier<QueryRunner> getQueryRunnerSupplier();

    Permission getPermission();
}
