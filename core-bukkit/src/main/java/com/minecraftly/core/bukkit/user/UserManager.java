package com.minecraftly.core.bukkit.user;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.minecraftly.core.bukkit.database.DatabaseManager;
import com.minecraftly.core.bukkit.user.modularisation.DataStorageHandler;
import com.minecraftly.core.bukkit.user.modularisation.UserData;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 15/03/2015.
 */
public class UserManager {

    private final Logger logger;
    private final Supplier<DatabaseManager> databaseManagerSupplier;
    private final Supplier<QueryRunner> queryRunnerSupplier = new Supplier<QueryRunner>() {
        @Override
        public QueryRunner get() {
            return databaseManagerSupplier.get().getQueryRunner();
        }
    };

    private final List<DataStorageHandler> dataStorageHandlers = new ArrayList<>();
    private final Map<UUID, User> loadedUsers = new ConcurrentHashMap<>();
    private final Cache<UUID, User> tempLoadedUsers = CacheBuilder.newBuilder().concurrencyLevel(4).softValues().build();

    public UserManager(Logger logger, Supplier<DatabaseManager> databaseManagerSupplier) {
        checkNotNull(logger);
        checkNotNull(databaseManagerSupplier);
        this.logger = logger;
        this.databaseManagerSupplier = databaseManagerSupplier;
    }

    public void addDataStorageHandler(DataStorageHandler dataStorageHandler) {
        dataStorageHandlers.add(dataStorageHandler);
        dataStorageHandler.initialize(queryRunnerSupplier);

        // if this storage handler is added at runtime (after some players have loaded), initialize them
        for (User user : getAllUsers()) {
            try {
                initializeStorageHandler(user, dataStorageHandler);
            } catch (Throwable throwable) {
                logger.log(Level.SEVERE,
                        "Error whilst initializing storage handler "
                                + dataStorageHandler.getClass().getName()
                                + " data for "
                                + user.getUniqueId()
                                + ".",
                        throwable
                );
            }
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        users.addAll(loadedUsers.values());
        users.addAll(tempLoadedUsers.asMap().values());
        return Collections.unmodifiableList(users);
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
            }
        } else if (user.isOnline()) { // user not null and online
            // move player from temporary cache to a more permanent cache (until they log off)
            tempLoadedUsers.invalidate(uuid);
            loadedUsers.put(uuid, user);
        }

        return user;
    }

    public User load(UUID uuid) {
        return load(uuid, Bukkit.getPlayer(uuid) != null);
    }

    public User load(UUID uuid, boolean online) {
        User user = new User(uuid);
        load(user);

        if (online) {
            loadedUsers.put(uuid, user);
        } else {
            tempLoadedUsers.put(uuid, user);
        }

        return user;
    }

    private void load(User user) {
        for (DataStorageHandler dataStorageHandler : dataStorageHandlers) {
            initializeStorageHandler(user, dataStorageHandler);
        }
    }

    private void initializeStorageHandler(User user, DataStorageHandler dataStorageHandler) {
        UserData userData = dataStorageHandler.instantiateUserData(user);

        if (!userData.load(queryRunnerSupplier)) {
            logger.warning("Error whilst loading " + userData.getClass().getName() + " data for " + user.getUniqueId() + ".");
        }

        user.attachUserData(userData);
    }

    public void unloadAll() {
        List<User> users = new ArrayList<>();
        users.addAll(loadedUsers.values());
        users.addAll(tempLoadedUsers.asMap().values());
        users.forEach(this::unload);
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
        for (UserData userData : user.getAttachedUserData()) {
            if (!userData.save(queryRunnerSupplier)) {
                logger.warning("Error whilst saving " + userData.getClass().getName() + " data for " + user.getUniqueId() + ".");
            }
        }
    }

}
