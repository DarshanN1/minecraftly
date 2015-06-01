package com.minecraftly.core.bukkit.user.modularisation;

import com.minecraftly.core.bukkit.user.User;
import org.apache.commons.dbutils.QueryRunner;

import java.util.function.Supplier;

/**
 * A piece of data attached to <b>one</b> {@link User}.
 */
public abstract class UserData {

    private final User user;

    /**
     * Creates a new instance.
     *
     * @param user the user to attach this data to
     */
    public UserData(User user) {
        this.user = user;
    }

    /**
     * Gets the user this data is attached to.
     *
     * @return the user
     */
    public final User getUser() {
        return user;
    }

    /**
     * Loads data from an SQL database.
     *
     * @param queryRunnerSupplier the supplier which provides a means of executing queries.
     * @return true if data loaded successfully, false if there was a fatal error
     */
    public abstract boolean load(Supplier<QueryRunner> queryRunnerSupplier);

    /**
     * Saves data to an SQL database.
     *
     * @param queryRunnerSupplier the supplier which provides a means of executing queries.
     * @return true if data saved successfully, false if there was a fatal error
     */
    public abstract boolean save(Supplier<QueryRunner> queryRunnerSupplier);

}
