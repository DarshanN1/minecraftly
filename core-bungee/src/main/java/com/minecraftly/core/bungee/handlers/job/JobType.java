package com.minecraftly.core.bungee.handlers.job;

import net.md_5.bungee.api.connection.Server;

/**
 * Created by Keir on 28/06/2015.
 */
public enum JobType {

    CONNECT(Server.class), IS_HUMAN(Boolean.class);

    private Class<?> classType;

    JobType(Class<?> classType) {
        this.classType = classType;
    }

    public Class<?> getClassType() {
        return classType;
    }
}
