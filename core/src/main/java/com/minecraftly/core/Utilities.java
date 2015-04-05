package com.minecraftly.core;

import java.io.File;

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

}
