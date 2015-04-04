package com.minecraftly.core.bukkit.module.exception;

import com.minecraftly.core.bukkit.module.Module;

/**
 * Created by Keir on 12/03/2015.
 * todo
 */
public class ModuleAlreadyLoadedException extends Exception {

    private static final long serialVersionUID = 1L;
    private Module loadedModule;

    public ModuleAlreadyLoadedException(Module loadedModule) {
        this.loadedModule = loadedModule;
    }

    public ModuleAlreadyLoadedException(String message, Module loadedModule) {
        super(message);
        this.loadedModule = loadedModule;
    }

    public ModuleAlreadyLoadedException(String message, Module loadedModule, Throwable cause) {
        super(message, cause);
        this.loadedModule = loadedModule;
    }

    public ModuleAlreadyLoadedException(Module loadedModule, Throwable cause) {
        super(cause);
        this.loadedModule = loadedModule;
    }

    public Module getLoadedModule() {
        return loadedModule;
    }
}
