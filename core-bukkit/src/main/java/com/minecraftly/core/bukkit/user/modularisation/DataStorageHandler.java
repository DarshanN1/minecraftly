package com.minecraftly.core.bukkit.user.modularisation;

import com.minecraftly.core.bukkit.user.User;
import org.apache.commons.dbutils.QueryRunner;

import java.util.function.Supplier;

/**
 * Handles auto-loading of {@link SingletonUserData} instances.
 * Another solution will be required to instantiate non-singleton {@link UserData} classes.
 */
public abstract class DataStorageHandler<T extends SingletonUserData> {

    public DataStorageHandler() {
    }

    public abstract void initialize(Supplier<QueryRunner> queryRunnerSupplier);

    public abstract T instantiateUserData(User user);

}
