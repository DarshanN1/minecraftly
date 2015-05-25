package com.minecraftly.core.bukkit.module;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.Utilities;
import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.module.exception.InvalidModuleDescriptionException;
import com.minecraftly.core.bukkit.module.exception.ModuleAlreadyLoadedException;
import com.minecraftly.core.bukkit.utilities.PrefixedLogger;
import com.sk89q.intake.fluent.DispatcherNode;
import org.bukkit.generator.ChunkGenerator;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for managing all modules.
 */
public class ModuleManager {

    private static final ClassLoader parentClassLoader = ModuleManager.class.getClassLoader();

    private final Logger logger;
    private final String moduleLoggerPrefix;
    private final File moduleFolder;

    private Map<String, Module> modules = new HashMap<>();

    public ModuleManager(Logger logger, String moduleLoggerPrefix, File moduleFolder) throws IOException {
        checkNotNull(logger);
        checkNotNull(moduleLoggerPrefix);
        checkNotNull(moduleFolder);

        this.logger = logger;
        this.moduleLoggerPrefix = moduleLoggerPrefix;
        this.moduleFolder = moduleFolder;

        Utilities.createDirectory(moduleFolder);
    }

    /**
     * Gets all loaded modules, both enabled and disabled.
     *
     * @return the modules
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    /**
     * Gets all enabled modules.
     *
     * @return the modules
     */
    public List<Module> getEnabledModules() {
        List<Module> activeModules = new ArrayList<>();

        for (Module module : getModules()) {
            if (module.isEnabled()) {
                activeModules.add(module);
            }
        }

        return activeModules;
    }

    /**
     * Gets a module by it's name.
     *
     * @param name the name of the module
     * @return the module (null if not found)
     */
    public Module getModule(String name) {
        for (Module module : getModules()) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }

    /**
     * Attempts to load all modules in the module folder.
     */
    public void loadModules() {
        File[] files = moduleFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar");
            }
        });

        for (File moduleJar : files) {
            try {
                loadModule(moduleJar);
            } catch (InvalidModuleDescriptionException e) {
                logger.log(Level.SEVERE, "Error occurred whilst loading \"" + moduleJar.getName() + "\" in modules directory", e);
            } catch (ModuleAlreadyLoadedException e) {
                logger.log(Level.SEVERE, "Duplicate modules detected: '" + e.getLoadedModule().getJarFile().getPath() + "', '" + moduleJar.getPath() + "'.");
            }
        }
    }

    /**
     * Attempts to enable all loaded modules.
     */
    public void enableModules() {
        for (Module module : modules.values()) {
            module.setEnabled(true);
        }
    }

    /**
     * Disables all enabled modules.
     */
    public void disableModules() {
        List<Module> modules = new ArrayList<>(this.modules.values());
        Collections.reverse(modules); // disable in reverse order

        for (Module module : modules) {
            module.setEnabled(false);
        }
    }

    /**
     * Attempts to load a module by it's file.
     *
     * @param moduleJar the jar file of the module
     * @return the module
     * @throws InvalidModuleDescriptionException thrown if there is an issue with the module description file
     * @throws ModuleAlreadyLoadedException thrown if the module is already loaded
     */
    public Module loadModule(File moduleJar) throws InvalidModuleDescriptionException, ModuleAlreadyLoadedException {
        URLClassLoader classLoader;

        try {
            classLoader = new URLClassLoader(new URL[]{moduleJar.toURI().toURL()}, parentClassLoader);
        } catch (MalformedURLException e) {
            throw new InvalidModuleDescriptionException("Unable to convert module jar path to URL for ClassLoader.", e);
        }

        JarFile jarFile;

        try {
            jarFile = new JarFile(moduleJar);
        } catch (IOException e) {
            throw new InvalidModuleDescriptionException(e);
        }

        JarEntry jarEntry = jarFile.getJarEntry("module.yml");

        if (jarEntry == null) {
            throw new InvalidModuleDescriptionException("Module jar doesn't contain module.yml");
        }

        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            Constructor constructor = new CustomClassLoaderConstructor(getClass().getClassLoader()); // works around ClassLoader issues
            ModuleProperties moduleProperties = new Yaml(constructor).loadAs(inputStream, ModuleProperties.class);
            String moduleName = moduleProperties.getName();
            Module loadedModule = getModule(moduleName); // check if module is already loaded

            if (loadedModule != null) {
                throw new ModuleAlreadyLoadedException("Module '" + moduleName + "' is already loaded.", loadedModule);
            }

            Class<?> jarClass;

            try {
                jarClass = Class.forName(moduleProperties.getMain(), true, classLoader);
            } catch (ClassNotFoundException e) {
                throw new InvalidModuleDescriptionException("Main class not found: " + moduleProperties.getMain() + ".", e);
            }

            Class<? extends Module> moduleClass;

            try {
                moduleClass = jarClass.asSubclass(Module.class);
            } catch (ClassCastException e) {
                throw new InvalidModuleDescriptionException("Main Class '" + jarClass.getName() + "' doesn't extend Module");
            }

            Module module = moduleClass.newInstance();
            logger.info("Loading module: " + moduleName + " version " + moduleProperties.getVersion());

            String prefix = moduleProperties.getPrefix();
            if (prefix == null) {
                prefix = moduleName;
            }

            prefix = "[" + moduleLoggerPrefix + ": " + prefix + "] ";
            module.init(new PrefixedLogger(moduleClass.getName(), prefix, logger), moduleProperties, moduleJar);

            try {
                module.onLoad(MclyCoreBukkitPlugin.getInstance()); // todo remove static get?
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error occurred whilst loading module: " + moduleName + " version " + moduleProperties.getVersion() + ".", e);
                return null;
            }

            modules.put(moduleProperties.getName(), module);
            return module;
        } catch (IOException e) {
            throw new InvalidModuleDescriptionException(e);
        } catch (IllegalAccessException e) {
            throw new InvalidModuleDescriptionException("No public empty constructor.", e);
        } catch (InstantiationException e) {
            throw new InvalidModuleDescriptionException("Error instantiating main class", e);
        }
    }

    /**
     * Registers the commands of all enabled modules via the Module#registerCommands method.
     *
     * @param dispatcherNode the dispatcher node to register the commands on
     */
    public void registerCommands(DispatcherNode dispatcherNode) {
        for (Module module : getEnabledModules()) {
            try {
                module.registerCommands(dispatcherNode);
            } catch (Exception e) {
                module.getLogger().log(Level.SEVERE, "Error occurred whilst registering commands.");
            }
        }
    }

    /**
     * Gets the appropriate chunk generator from the enabled modules.
     *
     * @param worldName the name of the world to generate
     * @param id the arguments for the creation of the world
     * @return the chunk generator (may be null if not found)
     */
    public ChunkGenerator getWorldGenerator(String worldName, String id) {
        ChunkGenerator chunkGenerator = null;

        if (!id.isEmpty()) {
            String[] parts = id.split(",");
            String moduleName = parts.length > 0 ? parts[0] : id;
            Module module = getModule(moduleName);

            if (module != null) {
                id = id.substring(moduleName.length() + (parts.length == 0 ? 0 : 1), id.length()); // strip module name
                chunkGenerator = module.getWorldGenerator(worldName, id);
            }
        }

        return chunkGenerator;
    }

}
