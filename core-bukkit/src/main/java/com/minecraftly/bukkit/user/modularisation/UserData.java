package com.minecraftly.bukkit.user.modularisation;

import com.minecraftly.bukkit.user.User;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * A piece of data attached to <b>one</b> {@link User}.
 */
public abstract class UserData {

    private final User user;
    private final Supplier<QueryRunner> queryRunnerSupplier;

    private boolean loaded = false;

    /**
     * Creates a new instance.
     *
     * @param user the user to attach this data to
     * @param queryRunnerSupplier used to supply a query runner
     */
    protected UserData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        this.user = user;
        this.queryRunnerSupplier = queryRunnerSupplier;
    }

    /**
     * Gets the user this data is attached to.
     *
     * @return the user
     */
    public final User getUser() {
        return user;
    }

    protected Supplier<QueryRunner> getQueryRunnerSupplier() {
        return queryRunnerSupplier;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void loadAndApply(Player player) {
        try {
            load();

            try {
                apply(player);
            } catch (Throwable e) {
                throw new RuntimeException("Error applying player data to player.", e); // todo exception type
            }
        } catch (Throwable e) {
            throw new RuntimeException("Error loading player data from SQL database.", e); // todo exception type
        }
    }

    protected void initialLoad() {
        Player player = getUser().getPlayer();

        if (player == null) {
            throw new UnsupportedOperationException("Cannot do initial load without player being online.");
        }

        extractFrom(player);
    }

    /**
     * Extracts data from a player and keeps it in the instance.
     *
     * @param player the player to extract the data from
     */
    public void extractFrom(Player player) {}

    /**
     * Loads data from an SQL database.
     */
    public void load() throws SQLException {
        if (loaded) {
            throw new UnsupportedOperationException("Attempted double load of UserData.");
        }

        loaded = true;
    }

    /**
     * Applies loaded data to a player (such as inventory).
     * This is called after load().
     *
     * @param player the player to apply the data to
     */
    public void apply(Player player) {}

    /**
     * Saves data to an SQL database.
     */
    public void save() throws SQLException {
        Player player = getUser().getPlayer();
        if (player != null) {
            extractFrom(player);
        }
    }

}
