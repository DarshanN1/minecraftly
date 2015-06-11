package com.minecraftly.core;

import java.io.File;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by Keir on 05/04/2015.
 */
public class Utilities {

    private Utilities(){}

    public static void createDirectory(File directory) {
        if (!directory.exists() && !directory.mkdir()) {
            throw new RuntimeException("Cannot create directory: '" + directory.getPath() + "'.");
        } else if (!directory.isDirectory()) {
            throw new RuntimeException("Path '" + directory.getPath() + "' is not a directory.");
        }
    }

    private static final Pattern UUID_DASH_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    public static UUID convertFromNoDashes(String uuidString) {
        return UUID.fromString(UUID_DASH_PATTERN.matcher(uuidString).replaceAll("$1-$2-$3-$4-$5"));
    }

    public static String convertToNoDashes(UUID uuid) {
        return convertToNoDashes(uuid.toString());
    }

    public static String convertToNoDashes(String uuidString) {
        return uuidString.replace("-", "");
    }

}
