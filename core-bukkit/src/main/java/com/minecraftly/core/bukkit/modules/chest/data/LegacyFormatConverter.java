package com.minecraftly.core.bukkit.modules.chest.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.bukkit.config.ConfigWrapper;
import com.minecraftly.core.bukkit.user.User;
import com.minecraftly.core.bukkit.user.UserManager;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 05/07/2015.
 */
public class LegacyFormatConverter {

    private static final String YAML_ENDING =".yml";

    private UserManager userManager;
    private File directory;
    private Logger logger;

    public LegacyFormatConverter(UserManager userManager, File directory, Logger logger) {
        this.userManager = checkNotNull(userManager);
        this.directory = checkNotNull(directory);
        this.logger = logger;

        if (!directory.exists()) {
            throw new IllegalArgumentException("Path doesn't exist.");
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory.");
        }
    }

    public void convert() {
        for (File file : directory.listFiles(f -> f.isFile() && f.getName().endsWith(YAML_ENDING))) {
            String fileName = file.getName();
            UUID uuid;

            try {
                uuid = UUID.fromString(fileName.substring(0, fileName.length() - YAML_ENDING.length()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            }

            User user = userManager.getUser(uuid);
            UserChestData userChestData = user.getSingletonUserData(UserChestData.class);
            ConfigWrapper configWrapper = new ConfigWrapper(file);
            List<Exception> exceptions = userChestData.parse(configWrapper.getConfig().getConfigurationSection("chests"));
            userManager.unload(user);
            logger.info("Converted legacy data for: " + uuid + ".");

            if (exceptions.size() > 0) {
                for (Exception exception : exceptions) {
                    logger.log(Level.SEVERE, "Error whilst converting data for: " + uuid, exception);
                    logger.log(Level.SEVERE, "########################################");
                }
            }
        }
    }

}
