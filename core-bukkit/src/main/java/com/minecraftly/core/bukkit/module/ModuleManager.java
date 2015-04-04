package com.minecraftly.core.bukkit.module;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.module.exception.InvalidModuleDescriptionException;
import com.minecraftly.core.bukkit.module.exception.ModuleAlreadyLoadedException;
import com.minecraftly.core.bukkit.utilities.PrefixedLogger;
import com.sk89q.intake.fluent.DispatcherNode;
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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

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

        if (!moduleFolder.exists()) {
            if (!moduleFolder.mkdir()) {
                throw new IOException("Unable to create module directory.");
            }
        }

        if (!moduleFolder.isDirectory()) {
            throw new IllegalArgumentException("File is not a directory.");
        }
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules.values());
    }

    public Module getModule(String name) {
        return modules.get(name);
    }

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

    public void enableModules() {
        for (Module module : modules.values()) {
            module.setEnabled(true);
        }
    }

    public void disableModules() {
        List<Module> modules = new ArrayList<>(this.modules.values());
        Collections.reverse(modules); // disable in reverse order

        for (Module module : modules) {
            module.setEnabled(false);
        }
    }

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

    public void registerCommands(DispatcherNode dispatcherNode) {
        for (Module module : getModules()) {
            if (module.isEnabled()) {
                try {
                    module.registerCommands(dispatcherNode);
                } catch (Exception e) {
                    module.getLogger().log(Level.SEVERE, "Error occurred whilst registering commands.");
                }
            }
        }
    }

}
