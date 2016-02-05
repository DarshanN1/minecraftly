package com.minecraftly.bukkit.commands;

import com.minecraftly.bukkit.language.LanguageManager;
import com.minecraftly.bukkit.user.User;
import com.minecraftly.bukkit.utilities.BukkitUtilities;
import com.minecraftly.bukkit.MinecraftlyCore;
import com.minecraftly.bukkit.config.ConfigWrapper;
import com.minecraftly.bukkit.language.LanguageValue;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import net.ellune.exhaust.command.annotation.Sender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Keir on 13/03/2015.
 */
public class MinecraftlyCommand {

    private LanguageValue langCleanupComplete = new LanguageValue("&6%s &bunused language values were cleaned.");
    private LanguageValue langCleanupCompletedBackup = new LanguageValue("&bBackup file saved to: &6%s");

    public MinecraftlyCommand(LanguageManager languageManager) {
        //noinspection serial
        languageManager.registerAll(new HashMap<String, LanguageValue>() {{
            String prefix = "core.command.minecraftly";
            String cleanupPrefix = prefix + ".cleanup";

            put(cleanupPrefix + ".cleanupComplete", langCleanupComplete);
            put(cleanupPrefix + ".cleanupCompleteBackup",langCleanupCompletedBackup );
        }});
    }

    @Command(aliases = "info", desc = "Displays information about the Minecraftly plugin", min = 0, max = 0)
    public void about(@Sender Player player, @Sender User user) {
        player.sendMessage(ChatColor.AQUA + "WIP."); // todo
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
