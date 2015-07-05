package com.minecraftly.core.bukkit.user.modularisation;

import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Handles auto-loading of {@link SingletonUserData} instances.
 * Another solution will be required to instantiate non-singleton {@link UserData} classes.
 */
public abstract class DataStorageHandler<T extends SingletonUserData> {

    private Supplier<QueryRunner> queryRunnerSupplier;

    public DataStorageHandler(Supplier<QueryRunner> queryRunnerSupplier) {
        this.queryRunnerSupplier = queryRunnerSupplier;
    }

    protected final Supplier<QueryRunner> getQueryRunnerSupplier() {
        return queryRunnerSupplier;
    }

    public void initialize() throws SQLException {}

}
