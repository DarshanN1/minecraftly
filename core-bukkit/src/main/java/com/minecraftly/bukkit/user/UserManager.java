package com.minecraftly.bukkit.user;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.minecraftly.bukkit.user.modularisation.AutoInitializedData;
import com.minecraftly.bukkit.user.modularisation.DataStorageHandler;
import com.minecraftly.bukkit.user.modularisation.UserData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Keir on 15/03/2015.
 */
public class UserManager {

    private final Logger logger;

    private final List<DataStorageHandler> dataStorageHandlers = new ArrayList<>();
    private final Map<UUID, User> loadedUsers = new ConcurrentHashMap<>();
    private final Cache<UUID, User> tempLoadedUsers = CacheBuilder.newBuilder().concurrencyLevel(4).softValues().build();

    public UserManager(Logger logger) {
        checkNotNull(logger);
        this.logger = logger;
    }

    public void addDataStorageHandler(DataStorageHandler dataStorageHandler) {
        dataStorageHandlers.add(dataStorageHandler);

        try {
            dataStorageHandler.initialize();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Unhandled SQLException whilst initializing data handler: " + dataStorageHandler.getClass().getName() + ".", e);
            return;
        }

        // if this storage handler is added at runtime (after some players have loaded), initialize them
        for (User user : getAllUsers()) {
            try {
                autoInitializeData(user, dataStorageHandler);
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

    public boolean isUserLoaded(UUID uuid) {
        return loadedUsers.containsKey(uuid) || tempLoadedUsers.asMap().containsKey(uuid);
    }

    public User getUser(OfflinePlayer offlinePlayer) {
        return getUser(offlinePlayer, true);
    }

    public User getUser(OfflinePlayer offlinePlayer, boolean loadIfNotLoaded) {
        return getUser(offlinePlayer.getUniqueId(), loadIfNotLoaded);
    }

    public User getUser(UUID uuid) {
        return getUser(uuid, true);
    }

    public User getUser(UUID uuid, boolean loadIfNotLoaded) {
        User user = tempLoadedUsers.getIfPresent(uuid);

        if (user == null) {
            user = loadedUsers.get(uuid);

            if (user == null && loadIfNotLoaded) {
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
            autoInitializeData(user, dataStorageHandler);
        }
    }

    private void autoInitializeData(User user, DataStorageHandler dataStorageHandler) {
        if (dataStorageHandler instanceof AutoInitializedData) {
            UserData userData = ((AutoInitializedData) dataStorageHandler).autoInitialize(user);

            try {
                if (!userData.isLoaded()) {
                    userData.load();
                    Player player = user.getPlayer();
                    if (player != null) {
                        userData.apply(player);
                    }

                    user.attachUserData(userData);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Unhandled SQLException whilst loading '" + user.getUniqueId() + "'.", e);
            }
        }
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
            try {
                userData.save();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Unhandled SQLException whilst saving '" + user.getUniqueId() + "'.", e);
            }
        }
    }

}
