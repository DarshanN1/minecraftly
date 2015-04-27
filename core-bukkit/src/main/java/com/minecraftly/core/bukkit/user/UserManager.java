package com.minecraftly.core.bukkit.user;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by Keir on 15/03/2015.
 */
public class UserManager {

    private final Logger logger;
    private final DatabaseManager databaseManager;

    private final Map<UUID, User> loadedUsers = new ConcurrentHashMap<>();
    private final Cache<UUID, User> tempLoadedUsers = CacheBuilder.newBuilder().concurrencyLevel(4).softValues().build();

    public UserManager(Logger logger, DatabaseManager databaseManager) {
        checkNotNull(logger);
        checkNotNull(databaseManager);
        this.databaseManager = databaseManager;
        this.logger = logger;

        try {
            databaseManager.getQueryRunner().update(
                    String.format(
                            "CREATE TABLE IF NOT EXISTS `%smain` (`uuid` BINARY(16) NOT NULL, `last_name` VARCHAR(16), PRIMARY KEY (`uuid`))",
                            databaseManager.getPrefix()
                    )
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUser(OfflinePlayer offlinePlayer) {
        return getUser(offlinePlayer.getUniqueId());
    }

    public User getUser(UUID uuid) {
        User user = tempLoadedUsers.getIfPresent(uuid);

        if (user == null) {
            user = loadedUsers.get(uuid);

            if (user == null) {
                user = load(uuid);

                if (user.isOnline()) {
                    loadedUsers.put(uuid, user);
                } else {
                    tempLoadedUsers.put(uuid, user);
                }
            }
        } else if (user.isOnline()) { // user not null and online
            // move player from temporary cache to a more permanent cache (until they log off)
            tempLoadedUsers.invalidate(uuid);
            loadedUsers.put(uuid, user);
        }

        return user;
    }

    private User load(UUID uuid) {
        User user = new User(uuid);
        load(user);
        return user;
    }

    private void load(User user) {
        // todo user loading
    }

    public void unloadAll() {
        List<User> users = new ArrayList<>();
        users.addAll(loadedUsers.values());
        users.addAll(tempLoadedUsers.asMap().values());

        for (User user : users) {
            unload(user);
        }
    }

    public void unload(User user) {
        unload(user, true);
    }

    public void unload(User user, boolean save) {
        if (save) {
            save(user);
        }

        UUID uuid = user.getUniqueId();
        loadedUsers.remove(uuid);
        tempLoadedUsers.invalidate(uuid);
    }

    public void save(User user) {
        // todo save
    }

}
