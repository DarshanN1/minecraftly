package com.minecraftly.core.bungee.utilities;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.minecraftly.core.bungee.MclyCoreBungeePlugin;
import com.minecraftly.core.utilities.Utilities;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of utility methods to aid in the writing of BungeeCord stuff.
 */
public class BungeeUtilities {

    private static MclyCoreBungeePlugin bungeePlugin = MclyCoreBungeePlugin.getInstance();
    private static Logger logger = bungeePlugin.getLogger();

    private BungeeUtilities() {}

    /**
     * Matches an input string against the names of all the online players in the network.
     *
     * @param input the input to find the best match for
     * @return the best match
     */
    public static String matchRedisPlayer(String input) {
        return match(input, RedisBungee.getApi().getHumanPlayersOnline());
    }

    /**
     * Matches a string against a collection of strings and returns the best match.
     * Stolen from Bukkit #getPlayer(String) method.
     *
     * @param input the input to match against the collection
     * @param collection the collection of full strings
     * @return the best match
     */
    public static String match(String input, Collection<String> collection) {
        String found = null;
        String lowerCase = input.toLowerCase();
        int delta = Integer.MAX_VALUE;

        for (String string : collection) {
            if (string.toLowerCase().startsWith(lowerCase)) {
                int curDelta = string.length() - lowerCase.length();
                if (curDelta < delta) {
                    found = string;
                    delta = curDelta;
                }

                if (curDelta == 0) break;
            }
        }

        return found;
    }

    public static ProxiedPlayer easyMatchPlayer(String name) {
        Collection<ProxiedPlayer> results = ProxyServer.getInstance().matchPlayer(name);
        return results.size() > 0 ? results.iterator().next() : null;
    }

    public static void setListenerInfoField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        for (ListenerInfo listenerInfo : ProxyServer.getInstance().getConfig().getListeners()) {
            Field field = ListenerInfo.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Utilities.removeFinal(field);
            field.set(listenerInfo, value);
        }
    }

    public static void copyDefaultsFromJarFile(Configuration configuration, String defaultFileName, ConfigurationProvider configurationProvider, File configFile) {
        copyDefaultsFromJarFile(configuration, configurationProvider.load(bungeePlugin.getResourceAsStream(defaultFileName)), configurationProvider, configFile);
    }

    public static void copyDefaultsFromJarFile(Configuration configuration, Configuration defaultConfiguration, ConfigurationProvider configurationProvider, File configFile) {
        boolean updated = false;

        for (String key : defaultConfiguration.getKeys()) {
            if (configuration.get(key) == null) {
                configuration.set(key, defaultConfiguration.get(key));
                updated = true;
            }
        }

        if (updated) {
            try {
                configurationProvider.save(configuration, configFile);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error saving configuration with new defaults to file.", e);
            }
        }
    }

}
