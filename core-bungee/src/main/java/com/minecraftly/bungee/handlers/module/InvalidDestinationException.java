package com.minecraftly.bungee.handlers.module;

/**
 * TODO
 * Created by ikeirnez on 25/01/16.
 */
public class InvalidDestinationException extends Exception {
    private static final long serialVersionUID = -3435080169316875487L;

    private final String input;

    public InvalidDestinationException(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
