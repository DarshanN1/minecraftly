package com.minecraftly.core.bukkit.commands;

import com.minecraftly.core.bukkit.MinecraftlyCore;
import com.minecraftly.core.bukkit.config.ConfigWrapper;
import com.minecraftly.core.bukkit.language.LanguageManager;
import com.minecraftly.core.bukkit.language.LanguageValue;
import com.minecraftly.core.bukkit.module.Module;
import com.minecraftly.core.bukkit.utilities.BukkitUtilities;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Keir on 15/03/2015.
 */
public class ModulesCommand {

    public static final String LANG_KEY_PREFIX = "core.command.modules.";

    public static final String LANG_VIEW_KEY_PREFIX = LANG_KEY_PREFIX + "view.";
    public static final String LANG_LOADED_MODULES = LANG_VIEW_KEY_PREFIX + "loadedModules";
    public static final String LANG_LOADED_MODULES_SEPARATOR = LANG_VIEW_KEY_PREFIX + "separator";
    public static final String LANG_LOADED_MODULE_ENABLED_PREFIX = LANG_VIEW_KEY_PREFIX + "moduleEnabledPrefix";
    public static final String LANG_LOADED_MODULE_DISABLED_PREFIX = LANG_VIEW_KEY_PREFIX + "moduleDisabledPrefix";
    public static final String LANG_NO_MODULES = LANG_VIEW_KEY_PREFIX + "noModules";

    public static final String LANG_CLEANUP_KEY_PREFIX = LANG_KEY_PREFIX + "cleanup.";
    public static final String LANG_FOUND_UNUSED_LANG_HEADER = LANG_CLEANUP_KEY_PREFIX + "foundUnusedLangHeader";
    public static final String LANG_CLEANUP_COMPLETE = LANG_CLEANUP_KEY_PREFIX + "cleanupComplete";
    public static final String LANG_CLEANUP_COMPLETE_BACKUP = LANG_CLEANUP_KEY_PREFIX + "cleanupCompleteBackup";

    private MinecraftlyCore plugin;
    private LanguageManager languageManager;

    public ModulesCommand(final MinecraftlyCore plugin) {
        this.plugin = plugin;
        languageManager = plugin.getLanguageManager();

        //noinspection serial
        languageManager.registerAll(new HashMap<String, LanguageValue>() {{
            put(LANG_LOADED_MODULES, new LanguageValue(plugin, "&bLoaded modules: "));
            put(LANG_LOADED_MODULES_SEPARATOR, new LanguageValue(plugin, "&7, "));
            put(LANG_LOADED_MODULE_ENABLED_PREFIX, new LanguageValue(plugin, "&a"));
            put(LANG_LOADED_MODULE_DISABLED_PREFIX, new LanguageValue(plugin, "&c"));
            put(LANG_NO_MODULES, new LanguageValue(plugin, "&cThere are no currently loaded modules."));

            put(LANG_FOUND_UNUSED_LANG_HEADER, new LanguageValue(plugin, "&bThe following keys and associated values have been found to be unused.\n&bThey will be backed up and then removed from the main language file."));
            put(LANG_CLEANUP_COMPLETE, new LanguageValue(plugin, "&6%s &bunused language values were cleaned."));
            put(LANG_CLEANUP_COMPLETE_BACKUP, new LanguageValue(plugin, "&bBackup file saved to: &6%s"));
        }});
    }

    @Command(aliases = "view", desc = "View all Minecraftly modules", max = 0)
    @Require("com.minecraftly.core.modules.view")
    public void viewModules(CommandSender sender) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Module> modules = plugin.getModuleManager().getModules();

        if (modules.size() > 0) {
            stringBuilder.append(languageManager.get(LANG_LOADED_MODULES));

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                stringBuilder.append(module.isEnabled() ? languageManager.get(LANG_LOADED_MODULE_ENABLED_PREFIX) :
                        languageManager.get(LANG_LOADED_MODULE_DISABLED_PREFIX))
                        .append(module.getName());

                if (i != modules.size() - 1) { // if not last item
                    stringBuilder.append(languageManager.get(LANG_LOADED_MODULES_SEPARATOR));
                }
            }
        } else {
            stringBuilder.append(languageManager.get(LANG_NO_MODULES));
        }

        sender.sendMessage(stringBuilder.toString());
    }

    @Command(aliases = "cleanup", desc = "Cleanup leftover junk from updated or removed modules", max = 0)
    @Require("com.minecraftly.core.modules.cleanup")
    public void cleanup(CommandSender sender, MinecraftlyCore minecraftlyCore, LanguageManager languageManager) {
        languageManager.save();
        FileConfiguration languageConfig = languageManager.config;

        File backupFile = new File(minecraftlyCore.getBackupsDirectory(), "language-" + BukkitUtilities.TIMESTAMP_FORMAT.format(new Date()) + ".yml");
        ConfigWrapper backupConfigWrapper = new ConfigWrapper(backupFile);
        FileConfiguration backupConfig = backupConfigWrapper.getConfig();
        int count = 0;

        for (String key : languageConfig.getKeys(true)) {
            if (!languageConfig.isConfigurationSection(key)) {
                if (languageManager.getRaw(key) == null) {
                    String value = languageConfig.getString(key);
                    backupConfig.set(key, value);
                    languageConfig.set(key, null);

                    String parent = key;
                    while (parent.contains(".")) { // remove empty sections caused by the removal of the above value
                        parent = parent.substring(0, parent.lastIndexOf('.'));

                        if (!languageConfig.isConfigurationSection(parent) || languageConfig.getConfigurationSection(parent).getKeys(true).size() > 0) {
                            break;
                        }

                        languageConfig.set(parent, null);
                    }
                } else {
                    continue; // don't increment count
                }
            } else if (languageConfig.getConfigurationSection(key).getKeys(true).size() == 0) { // empty configuration section
                languageConfig.set(key, null);
            } else {
                continue; // don't increment count
            }

            count++;
        }

        sender.sendMessage(languageManager.get(LANG_CLEANUP_COMPLETE, count));
        languageManager.configWrapper.saveConfig();

        // todo cleanup config

        if (backupConfig.getKeys(true).size() > 0) {
            backupConfigWrapper.saveConfig();
            sender.sendMessage(languageManager.get(LANG_CLEANUP_COMPLETE_BACKUP, backupFile.getPath()));
        } else { // if nothing was cleaned (backed up), delete backup
            backupFile.delete();
        }
    }

}
