package com.minecraftly.utilities;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

    public static boolean deleteDirectory(File directory) {
        checkNotNull(directory);
        checkArgument(directory.isDirectory(), "File is not directory.");

        try {
            Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
                @Nonnull
                @Override
                public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Nonnull
                @Override
                public FileVisitResult postVisitDirectory(@Nonnull Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void removeFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

}
