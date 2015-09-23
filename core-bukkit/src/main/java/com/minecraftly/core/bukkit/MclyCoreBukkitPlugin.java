package com.minecraftly.core.bukkit;

import com.google.gson.Gson;
import com.ikeirnez.pluginmessageframework.bukkit.BukkitGatewayProvider;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.MinecraftlyCommon;
import com.minecraftly.core.bukkit.commands.MinecraftlyCommand;
import com.minecraftly.core.bukkit.config.ConfigManager;
import com.minecraftly.core.bukkit.config.DataValue;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.internal.intake.MinecraftlyModule;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.listeners.PacketListener;
import com.minecraftly.core.bukkit.modules.Module;
import com.minecraftly.core.bukkit.modules.chest.ModuleChest;
import com.minecraftly.core.bukkit.modules.fun.ModuleFun;
import com.minecraftly.core.bukkit.modules.playerworlds.ModulePlayerWorlds;
import com.minecraftly.core.bukkit.modules.readonlyworlds.DoNothingWorldGenerator;
import com.minecraftly.core.bukkit.modules.readonlyworlds.ModuleReadOnlyWorlds;
import com.minecraftly.core.bukkit.modules.spawn.ModuleSpawn;
import com.minecraftly.core.bukkit.user.UserListener;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.bukkit.utilities.PrefixedLogger;
import com.minecraftly.core.healthcheck.HealthCheckWebServer;
import com.minecraftly.core.redis.RedisHelper;
import com.minecraftly.core.redis.message.gson.GsonHelper;
import com.minecraftly.core.utilities.ComputeEngineHelper;
import com.minecraftly.core.utilities.Utilities;
import com.sk89q.intake.fluent.DispatcherNode;
import lc.vq.exhaust.bukkit.command.CommandManager;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Created by Keir on 08/03/2015.
 */
public class MclyCoreBukkitPlugin extends JavaPlugin implements MinecraftlyCore {

    private static MclyCoreBukkitPlugin instance;

    // Google Compute
    private String computeUniqueId;
    private InetSocketAddress instanceExternalSocketAddress;
    private HealthCheckWebServer healthCheckWebServer;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private CommandManager commandManager;
    private UserManager userManager;
    private PluginManager pluginManager;
    private ServerGateway<Player> gateway;
    private PlayerSwitchJobManager playerSwitchJobManager;
    private JedisService jedisService;
    private File generalDataDirectory = new File(getDataFolder(), "data");
    private File backupDirectory = new File(getDataFolder(), "backups");
    private boolean skipDisable = false;

    private Permission permission;
    private Gson gson = GsonHelper.getGsonWithAdapters();

    private List<Module> modules = new ArrayList<>();

    public final DataValue<String> CFG_DB_HOST = new DataValue<>("127.0.0.1", String.class);
    public final DataValue<Integer> CFG_DB_PORT = new DataValue<>(3306, Integer.class);
    public final DataValue<String> CFG_DB_USER = new DataValue<>("root", String.class);
    public final DataValue<String> CFG_DB_PASS = new DataValue<>("", String.class);
    public final DataValue<String> CFG_DB_DATABASE = new DataValue<>("minecraftly", String.class);

    public final DataValue<String> CFG_JEDIS_HOST = new DataValue<>("localhost", String.class);
    public final DataValue<Integer> CFG_JEDIS_PORT = new DataValue<>(6379, Integer.class);
    public final DataValue<String> CFG_JEDIS_PASS = new DataValue<>("", String.class);

    public final DataValue<String> CFG_DEBUG_UNIQUE_ID = new DataValue<>("-1", String.class);
    public final DataValue<String> CFG_DEBUG_IP_ADDRESS = new DataValue<>("", String.class);
    public final DataValue<Boolean> CFG_DEBUG_SKIP_RSYNC = new DataValue<>(false, Boolean.class);
    public final DataValue<Integer> CFG_DEBUG_WEB_PORT = new DataValue<>(81, Integer.class);

    private final Map<String, DataValue> configValues = new HashMap<String, DataValue>() {{
        String dbPrefix = "database.";
        String jedisPrefix = "jedis.";
        String debugPrefix = "debug.";

        put(dbPrefix + "host", CFG_DB_HOST);
        put(dbPrefix + "port", CFG_DB_PORT);
        put(dbPrefix + "username", CFG_DB_USER);
        put(dbPrefix + "password", CFG_DB_PASS);
        put(dbPrefix + "database", CFG_DB_DATABASE);

        put(jedisPrefix + "host", CFG_JEDIS_HOST);
        put(jedisPrefix + "port", CFG_JEDIS_PORT);
        put(jedisPrefix + "password", CFG_JEDIS_PASS);

        put(debugPrefix + "uniqueId", CFG_DEBUG_UNIQUE_ID);
        put(debugPrefix + "ipAddress", CFG_DEBUG_IP_ADDRESS);
        put(debugPrefix + "skipRSync", CFG_DEBUG_SKIP_RSYNC);
        put(debugPrefix + "webPort", CFG_DEBUG_WEB_PORT);
    }};

    private final Supplier<QueryRunner> queryRunnerSupplier = () -> getDatabaseManager().getQueryRunner();

    {
        instance = this;
    }

    public static MclyCoreBukkitPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        BukkitScheduler scheduler = getServer().getScheduler();
        pluginManager = getServer().getPluginManager();
        RegisteredServiceProvider<Permission> economyServiceProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (economyServiceProvider == null) {
            getLogger().severe("Permission service not found.");
            pluginManager.disablePlugin(this);
            Bukkit.shutdown();
            return;
        }

        if (pluginManager.getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib not found.");
            pluginManager.disablePlugin(this);
            Bukkit.shutdown();
            return;
        }

        permission = economyServiceProvider.getProvider();

        File configurationFile = new File(getDataFolder(), "config.yml");
        boolean firstRun = !configurationFile.exists();

        configManager = new ConfigManager(configurationFile);
        configManager.registerAll(configValues);

        if (firstRun) { // display warning and disable plugin instead of nasty errors
            getLogger().severe("This is the first time this plugin has run and therefore no configuration exists for it.");
            getLogger().severe("Please configure settings such as database connections in the configuration.");
            getLogger().severe("Once done start the server up again and watch.");
            getLogger().severe("Configuration defaults have been copied to: '" + configurationFile.getPath() + "'.");

            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        }

        try {
            computeUniqueId = CFG_DEBUG_UNIQUE_ID.isValueDefault()
                    ? ComputeEngineHelper.queryUniqueId()
                    : CFG_DEBUG_UNIQUE_ID.getValue();

            int port = Bukkit.getPort();
            instanceExternalSocketAddress = InetSocketAddress.createUnresolved(
                    CFG_DEBUG_IP_ADDRESS.isValueDefault()
                            ? ComputeEngineHelper.queryIpAddress()
                            : CFG_DEBUG_IP_ADDRESS.getValue(),
                    port
            );

            getLogger().info("Instance ID - " + computeUniqueId);
            getLogger().info("Instance Address - " + instanceExternalSocketAddress.toString());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error fetching from Compute API.", e);
            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        } catch (NumberFormatException e) {
            getLogger().log(Level.SEVERE, "Error parsing Compute API response.", e);
            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        }

        healthCheckWebServer = new HealthCheckWebServer(CFG_DEBUG_WEB_PORT.getValue(), (r) -> scheduler.runTaskLater(this, r, 2L));
        jedisService = new JedisService(computeUniqueId, instanceExternalSocketAddress, CFG_JEDIS_HOST.getValue(), CFG_JEDIS_PORT.getValue(), CFG_JEDIS_PASS.getValue());
        scheduler.runTaskTimer(this, jedisService::heartbeat, 20L * RedisHelper.HEARTBEAT_INTERVAL, 20L * RedisHelper.HEARTBEAT_INTERVAL);
        scheduler.runTask(this, () -> jedisService.instanceAlive(gson)); // delay to next tick so that broadcast will be made when all plugins are enabled

        try {
            languageManager = new LanguageManager(BukkitUtilities.getLogger(this, LanguageManager.class, "Language"), new File(getDataFolder(), "language.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error whilst initializing language manager.", e);
            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        }

        Utilities.createDirectory(generalDataDirectory);
        Utilities.createDirectory(backupDirectory);

        gateway = BukkitGatewayProvider.getGateway(MinecraftlyCommon.GATEWAY_CHANNEL, this);
        playerSwitchJobManager = new PlayerSwitchJobManager(this, gateway);
        gateway.registerListener(new PacketListener());

        userManager = new UserManager(BukkitUtilities.getLogger(this, UserManager.class, "User Manager"));
        UserListener.initialize(this, userManager, playerSwitchJobManager);

        modules.add(new ModuleReadOnlyWorlds(this));
        modules.add(new ModulePlayerWorlds(this));
        modules.add(new ModuleSpawn(this));
        modules.add(new ModuleChest(this));
        modules.add(new ModuleFun(this));

        modules.forEach(Module::onLoad);
        DispatcherNode dispatcherNode = registerCoreCommands();

        modules.forEach(Module::onEnable);
        modules.forEach(m -> m.registerCommands(dispatcherNode));
        commandManager.build();

        languageManager.save(); // saves any new language values to file
    }

    private DispatcherNode registerCoreCommands() {
        commandManager = new CommandManager(this);
        commandManager.injector().install(new MinecraftlyModule(this));

        DispatcherNode dispatcherNode = commandManager.builder();
        dispatcherNode.group("minecraftly", "mcly")
                .registerMethods(new MinecraftlyCommand(languageManager));

        return dispatcherNode;
    }

    @Override
    public void onDisable() {
        if (configManager != null) {
            configManager.save();
        }

        if (!skipDisable) {
            if (healthCheckWebServer != null) {
                healthCheckWebServer.stop();
                healthCheckWebServer = null;
            }

            if (jedisService != null) {
                jedisService.destroy();
                jedisService = null;
            }

            userManager.unloadAll();
            modules.forEach(Module::onDisable);
            languageManager.save();

            if (databaseManager != null) {
                databaseManager.disconnect();
                databaseManager = null;
            }
        }

        instance = null;
    }

    // forward commands to Exhaust and then to Intake to be dispatched
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.commandManager.getDefaultExecutor().onCommand(sender, command, label, args);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new DoNothingWorldGenerator();
    }

    @Override
    public String getComputeUniqueId() {
        return computeUniqueId;
    }

    @Override
    public InetSocketAddress getInstanceExternalSocketAddress() {
        return instanceExternalSocketAddress;
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = new DatabaseManager(
                    new PrefixedLogger(DatabaseManager.class.getName(), "[" + getName() + ": Database] ", getLogger()),
                    CFG_DB_HOST.getValue(),
                    CFG_DB_USER.getValue(),
                    CFG_DB_PASS.getValue(),
                    CFG_DB_DATABASE.getValue(),
                    CFG_DB_PORT.getValue()
            );

            try {
                databaseManager.connect();
            } catch (Throwable throwable) {
                getLogger().log(Level.SEVERE, "Error connecting to database: " + throwable.getMessage());
                throw throwable;
            }
        }

        return databaseManager;
    }

    @Override
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    @Override
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public File getGeneralDataDirectory() {
        return generalDataDirectory;
    }

    @Override
    public File getBackupsDirectory() {
        return backupDirectory;
    }

    @Override
    public ServerGateway<Player> getGateway() {
        return gateway;
    }

    @Override
    public PlayerSwitchJobManager getPlayerSwitchJobManager() {
        return playerSwitchJobManager;
    }

    @Override
    public Supplier<QueryRunner> getQueryRunnerSupplier() {
        return queryRunnerSupplier;
    }

    @Override
    public Permission getPermission() {
        return permission;
    }
}
