package com.minecraftly.core.bukkit.user.modularisation;

import com.minecraftly.core.bukkit.user.User;
import org.apache.commons.dbutils.QueryRunner;

import java.util.function.Supplier;

/**
 * A {@link UserData} instance of which there will only be 1 instance of for each {@link User}.
 */
public abstract class SingletonUserData extends UserData {

    public SingletonUserData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

}
