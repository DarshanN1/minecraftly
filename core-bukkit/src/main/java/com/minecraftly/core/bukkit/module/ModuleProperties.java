package com.minecraftly.core.bukkit.module;

import com.google.common.base.Strings;

/**
 * POJO representing the values of a modules properties (module.yml)
 */
public class ModuleProperties implements Cloneable {

    private String name;
    private String identifier;
    private String version;
    private String main;
    private String prefix = null;

    public ModuleProperties() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        if (Strings.isNullOrEmpty(identifier)) {
            identifier = name.toLowerCase().replaceAll(" ", "-");
        }

        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public ModuleProperties clone() throws CloneNotSupportedException {
        return (ModuleProperties) super.clone();
    }
}
