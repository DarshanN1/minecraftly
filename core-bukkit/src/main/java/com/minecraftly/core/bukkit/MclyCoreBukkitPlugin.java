package com.minecraftly.core.bukkit;

import com.google.common.collect.ImmutableList;
import com.ikeirnez.pluginmessageframework.bukkit.BukkitGatewayProvider;
import com.ikeirnez.pluginmessageframework.gateway.ServerGateway;
import com.minecraftly.core.MinecraftlyCommon;
import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.commands.MinecraftlyCommand;
import com.minecraftly.core.bukkit.commands.ModulesCommand;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.internal.intake.MinecraftlyBinding;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.SimpleLanguageManager;
import com.minecraftly.core.bukkit.listeners.PacketListener;
import com.minecraftly.core.bukkit.module.ModuleManager;
import com.minecraftly.core.bukkit.user.UserManager;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.minecraftly.core.bukkit.utilities.PrefixedLogger;
import com.sk89q.intake.Parameter;
import com.sk89q.intake.SettableDescription;
import com.sk89q.intake.SettableParameter;
import com.sk89q.intake.fluent.DispatcherNode;
import lc.vq.exhaust.bukkit.command.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 08/03/2015.
 */
public class MclyCoreBukkitPlugin extends JavaPlugin implements MinecraftlyCore {

    private static MclyCoreBukkitPlugin instance;
    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private CommandManager commandManager;
    private UserManager userManager;
    private ModuleManager moduleManager;
    private PluginManager pluginManager;
    private ServerGateway<Player> gateway;
    private PreSwitchJobManager preSwitchJobManager = new PreSwitchJobManager();
    private File generalDataDirectory = new File(getDataFolder(), "data");
    private File backupDirectory = new File(getDataFolder(), "backups");
    private boolean skipDisable = false;

    {
        instance = this;
    }

    public static MinecraftlyCore getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        pluginManager = getServer().getPluginManager();

        File configurationFile = new File(getDataFolder(), "config.yml");
        boolean firstRun = !configurationFile.exists();

        getConfig().options().copyDefaults(true);
        saveConfig();

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
            languageManager = new SimpleLanguageManager(BukkitUtilities.getLogger(this, SimpleLanguageManager.class, "Language"), new File(getDataFolder(), "language_en.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error whilst initializing language manager.", e);
            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        }

        Utilities.createDirectory(generalDataDirectory);
        Utilities.createDirectory(backupDirectory);

        try {
            connectDatabase();
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Error connecting to database, disabling plugin...");
            skipDisable = true;
            pluginManager.disablePlugin(this);
            return;
        }

        userManager = new UserManager(BukkitUtilities.getLogger(this, UserManager.class, "User Manager"), databaseManager);

        try {
            moduleManager = new ModuleManager(getLogger(), getName(), new File(getDataFolder(), "modules"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Issue initializing module system.", e);
            pluginManager.disablePlugin(this);
            return;
        }

        gateway = BukkitGatewayProvider.getGateway(MinecraftlyCommon.GATEWAY_CHANNEL, this);
        gateway.registerListener(new PacketListener());
        gateway.registerListener(preSwitchJobManager);

        moduleManager.loadModules();
        DispatcherNode dispatcherNode = registerCoreCommands();

        moduleManager.enableModules();
        moduleManager.registerCommands(dispatcherNode);
        commandManager.build();

        languageManager.save(); // saves any new language values to file
    }

    private DispatcherNode registerCoreCommands() {
        commandManager = new CommandManager(this);
        commandManager.config().addBinding(new MinecraftlyBinding(this));

        DispatcherNode dispatcherNode = commandManager.builder();
        dispatcherNode.registerMethods(new MinecraftlyCommand());

        SettableDescription description = (SettableDescription) dispatcherNode.group("modules")
                .describeAs("Minecraftly module management commands")
                .registerMethods(new ModulesCommand(this))
                .getDispatcher().getDescription();

        description.setParameters(ImmutableList.<Parameter>builder()
                .add(new SettableParameter("view/cleanup"))
                .build());

        return dispatcherNode;
    }

    private void connectDatabase() {
        Logger databaseLogger = new PrefixedLogger(DatabaseManager.class.getName(), "[" + getName() + ": Database] ", getLogger());
        databaseManager = new DatabaseManager(databaseLogger, getConfig().getConfigurationSection("database").getValues(true));
        databaseManager.connect();
    }

    @Override
    public void onDisable() {
        if (!skipDisable) {
            moduleManager.disableModules();

            // absolute last tasks
            userManager.unloadAll();
            databaseManager.disconnect();
            languageManager.save();
        }

        instance = null;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return moduleManager.getWorldGenerator(worldName, id);
    }

    // forward commands to Exhaust and then to Intake to be dispatched
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.commandManager.getDefaultExecutor().onCommand(sender, command, label, args);
    }

    @Override
    public DatabaseManager getDatabaseManager() {
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
    public ModuleManager getModuleManager() {
        return moduleManager;
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
    public PreSwitchJobManager getPreSwitchJobManager() {
        return preSwitchJobManager;
    }

    @Override
    public String getIdentifier() {
        return "core-bukkit"; // todo retrieve from gradle?
    }
}
