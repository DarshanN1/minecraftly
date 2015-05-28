package com.minecraftly.core.bungee.utilities;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;

/**
 * A collection of utility methods to aid in the writing of BungeeCord stuff.
 */
public class BungeeUtilities {

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

}
