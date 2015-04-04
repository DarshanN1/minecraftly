package com.minecraftly.core.bukkit.module.exception;

/**
 * Created by Keir on 09/03/2015.
 * todo
 */
public class InvalidModuleDescriptionException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidModuleDescriptionException() {
    }

    public InvalidModuleDescriptionException(String message) {
        super(message);
    }

    public InvalidModuleDescriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidModuleDescriptionException(Throwable cause) {
        super(cause);
    }
}
