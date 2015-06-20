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

    private MinecraftlyCore plugin;

    private LanguageValue langLoadedModules = new LanguageValue("&bLoaded modules: ");
    private LanguageValue langLoadedModulesSeparator = new LanguageValue("&7, ");
    private LanguageValue langLoadedModuleEnabledPrefix = new LanguageValue("&a");
    private LanguageValue langLoadedModuleDisabledPrefix = new LanguageValue("&c");
    private LanguageValue langNoModules = new LanguageValue("&cThere are no currently loaded modules.");

    private LanguageValue langFoundUnusedLangHeader = new LanguageValue("&bThe following keys and associated values have been found to be unused.\n&bThey will be backed up and then removed from the main language file.");
    private LanguageValue langCleanupComplete = new LanguageValue("&6%s &bunused language values were cleaned.");
    private LanguageValue langCleanupCompletedBackup = new LanguageValue("&bBackup file saved to: &6%s");

    public ModulesCommand(final MinecraftlyCore plugin) {
        this.plugin = plugin;

        //noinspection serial
        this.plugin.getLanguageManager().registerAll(new HashMap<String, LanguageValue>() {{
            String prefix = "core.command.modules";

            String viewPrefix = prefix + ".view";
            put(viewPrefix + ".loadedModules", langLoadedModules);
            put(viewPrefix + ".separator", langLoadedModulesSeparator);
            put(viewPrefix + ".moduleEnabledPrefix", langLoadedModuleEnabledPrefix);
            put(viewPrefix + ".moduleDisabledPrefix", langLoadedModuleDisabledPrefix);
            put(viewPrefix + ".noModules", langNoModules);

            String cleanupPrefix = prefix + ".cleanup";
            put(cleanupPrefix + ".foundUnusedLangHeader", langFoundUnusedLangHeader);
            put(cleanupPrefix + ".cleanupComplete", langCleanupComplete);
            put(cleanupPrefix + ".cleanupCompleteBackup",langCleanupCompletedBackup );
        }});
    }

    @Command(aliases = "view", desc = "View all Minecraftly modules", max = 0)
    @Require("com.minecraftly.core.modules.view")
    public void viewModules(CommandSender sender) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Module> modules = plugin.getModuleManager().getModules();

        if (modules.size() > 0) {
            stringBuilder.append(langLoadedModules.getValue());

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                stringBuilder.append(module.isEnabled() ? langLoadedModuleEnabledPrefix.getValue() :
                        langLoadedModuleDisabledPrefix.getValue())
                        .append(module.getName());

                if (i != modules.size() - 1) { // if not last item
                    stringBuilder.append(langLoadedModulesSeparator.getValue());
                }
            }
        } else {
            stringBuilder.append(langNoModules.getValue());
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

        langCleanupComplete.send(sender, count);
        languageManager.configWrapper.saveConfig();

        // todo cleanup config

        if (backupConfig.getKeys(true).size() > 0) {
            backupConfigWrapper.saveConfig();
            langCleanupCompletedBackup.send(sender, backupFile.getPath());
        } else { // if nothing was cleaned (backed up), delete backup
            backupFile.delete();
        }
    }

}
