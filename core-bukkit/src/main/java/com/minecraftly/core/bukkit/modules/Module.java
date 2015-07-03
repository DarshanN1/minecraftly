package com.minecraftly.core.bukkit.modules;

import com.minecraftly.core.bukkit.MclyCoreBukkitPlugin;
import com.minecraftly.core.bukkit.utilities.PrefixedLogger;
import com.sk89q.intake.fluent.DispatcherNode;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;

/**
 * Created by Keir on 02/07/2015.
 */
public abstract class Module {

    private static final String MODULE_LANG_SECTION = "module";

    private String name;
    private Logger logger;
    private String languageSection;
    private WeakReference<MclyCoreBukkitPlugin> plugin = null;

    public Module(String name, MclyCoreBukkitPlugin plugin) {
        this.name = name;
        this.logger = new PrefixedLogger(getClass().getName(), "[" + name + "] ", plugin.getLogger());
        this.languageSection = MODULE_LANG_SECTION + "." + StringUtils.capitalize(getName().toLowerCase().replace("_", " ").replace("-", " ")).replace(" ", "");
        this.plugin = new WeakReference<>(plugin);
    }

    public MclyCoreBukkitPlugin getPlugin() {
        return plugin != null ? plugin.get() : null;
    }

    public String getName() {
        return name;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getLanguageSection() {
        return languageSection;
    }

    public final void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, MclyCoreBukkitPlugin.getInstance());
    }

    public void onLoad() {}
    public void onEnable() {}
    public void onDisable() {}

    public void registerCommands(DispatcherNode dispatcherNode) {}

}
