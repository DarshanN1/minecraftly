package com.minecraftly.core.bukkit.utilities;

/**
 * A {@link java.util.function.Consumer} like class but allowing for exceptions to be thrown.
 */
public interface ExceptionalConsumer<T> {

    void accept(T t) throws Throwable;

}
