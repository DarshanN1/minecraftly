package com.minecraftly.core.bukkit.utilities;

import static com.google.common.base.Preconditions.checkArgument;

import com.minecraftly.core.packets.LocationContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Keir on 20/03/2015.
 */
public class BukkitUtilities {

    public static final char COLOR_CHAR = '\u00A7';
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private static final Pattern VALID_VERSION_STRING_PATTERN = Pattern.compile("^([0-9]+(\\.[0-9])*)+[a-z]?$");

    private BukkitUtilities() {
    }

    /**
     * Compares 2 version strings numerically.
     * This may be used when calculating which version is newest.
     * <p/>
     * Input version strings must match the pattern <number>.<number>.<number><single lowercase character>.
     * The precision of the string can be as deep as you wish. The single lowercase character is optional but must be at the end of the string.
     *
     * @param versionString1 the first version string to be compared.
     * @param versionString2 the second version string to be compared.
     * @return returns -1, 0 or 1 if versionString1 is smaller, equal or greater than versionString2 respectively.
     * @throws java.lang.IllegalArgumentException if one of the input strings does not match the pattern above
     */
    public static int compareVersions(String versionString1, String versionString2) throws IllegalArgumentException {
        checkArgument(VALID_VERSION_STRING_PATTERN.matcher(versionString1).matches(), "First input '" + versionString1 + "' is not a valid version string.");
        checkArgument(VALID_VERSION_STRING_PATTERN.matcher(versionString2).matches(), "Second input '" + versionString2 + "' is not a valid version string.");

        if (versionString1.equals(versionString2)) return 0; // early exit if both strings match

        char endChar1 = versionString1.charAt(versionString1.length() - 1);
        char endChar2 = versionString2.charAt(versionString2.length() - 1);

        if (Character.isAlphabetic(endChar1) && Character.isLowerCase(endChar1)) {
            versionString1 = versionString1.substring(0, versionString1.length() - 1);
        } else {
            endChar1 = 0;
        }

        if (Character.isAlphabetic(endChar2) && Character.isLowerCase(endChar2)) {
            versionString2 = versionString2.substring(0, versionString2.length() - 1);
        } else {
            endChar2 = 0;
        }

        String[] split1 = versionString1.split(Pattern.quote("."));
        String[] split2 = versionString2.split(Pattern.quote("."));

        // compare int by int until we find a pair which are in-equal
        for (int i = 0; i < split1.length && i < split2.length; i++) {
            Integer num1 = Integer.parseInt(split1[i]);
            Integer num2 = Integer.parseInt(split2[i]);

            int compare = num1.compareTo(num2);
            if (compare != 0) return compare;
        }

        return Integer.compare(endChar1, endChar2);
    }

    public static Logger getLogger(Plugin plugin, Class<?> clazz, String name) {
        return new PrefixedLogger(clazz.getName(), "[" + plugin.getName() + ": " + name + "]", plugin.getLogger());
    }

    /**
     * Translates a string using an alternate color code character into a
     * string that uses the internal ChatColor.COLOR_CODE color code
     * character. The alternate color code character will only be replaced if
     * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
     *
     * @param altColorChar    The alternate color code character to replace. Ex: &
     * @param textToTranslate Text containing the alternate color code character.
     * @return Text containing the ChatColor.COLOR_CODE color code character.
     */
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    /**
     * Converts a {@link LocationContainer} to a Bukkit {@link Location}.
     *
     * @param locationContainer the location container to convert
     * @return the bukkit location
     */
    public static Location getLocation(LocationContainer locationContainer) {
        return new Location(
                Bukkit.getWorld(locationContainer.getWorld()),
                locationContainer.getX(),
                locationContainer.getY(),
                locationContainer.getZ(),
                locationContainer.getYaw(),
                locationContainer.getPitch()
        );
    }

    public static Location getLocation(ConfigurationSection configurationSection) {
        return getLocation(configurationSection.getValues(true));
    }

    public static Location getLocation(Map<String, Object> data) {
        return getLocation(new LocationContainer(data));
    }

    public static LocationContainer getLocationContainer(Location location) {
        return new LocationContainer(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    /**
     * Takes a location and gets the nearest safest location (above) to spawn a player.
     *
     * @param location the original location
     * @return the safe location
     */
    public static Location getSafeLocation(Location location) {
        while (location.getBlock().getType() != Material.AIR || location.add(0, 1, 0).getBlock().getType() != Material.AIR) {
            location = location.add(0, 1, 0);
        }

        return location;
    }

    public static ConfigurationSection getOrCreateSection(ConfigurationSection configurationSection, String sectionName) {
        return configurationSection.contains(sectionName) ? configurationSection.getConfigurationSection(sectionName) : configurationSection.createSection(sectionName);
    }

    public static void broadcast(List<Player> playerList, String message) {
        broadcast(playerList, null, message);
    }

    public static void broadcast(List<Player> playerList, Player exclude, String message) {
        for (Player player : playerList) {
            if (player != exclude) {
                player.sendMessage(message);
            }
        }
    }

}
