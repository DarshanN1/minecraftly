package com.minecraftly.core.bukkit.user;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.bukkit.user.modularisation.SingletonUserData;
import com.minecraftly.core.bukkit.user.modularisation.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Keir on 13/03/2015.
 */
public class User {

    private final UUID uuid;
    private SoftReference<Player> playerSoftReference = new SoftReference<Player>(null);

    private Map<Class<? extends UserData>, Set<UserData>> attachedUserData = new HashMap<>(); // map with class as key prevents duplicate values

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        if (playerSoftReference == null || playerSoftReference.get() == null) {
            Player player = Bukkit.getPlayer(getUniqueId());

            if (player != null) {
                this.playerSoftReference = new SoftReference<>(player);
            }
        }

        return playerSoftReference.get();
    }

    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }

    public Collection<UserData> getAttachedUserData() {
        List<UserData> userDataList = new ArrayList<>();
        attachedUserData.values().forEach(userDataList::addAll);
        return Collections.unmodifiableList(userDataList);
    }

    /**
     * Attaches a {@link UserData} to this user.
     *
     * @param userData the data to attach
     * @throws IllegalArgumentException thrown if an attempt is made to attach data to this instance that is not owned by the instance
     * @throws UnsupportedOperationException thrown if an attempt is made to register multiple instances of a {@link SingletonUserData} class.
     */
    public void attachUserData(UserData userData) throws IllegalArgumentException, UnsupportedOperationException {
        checkNotNull(userData);

        if (userData.getUser() != this) {
            throw new IllegalArgumentException("Cannot attach user data which is not owned by this user instance.");
        }

        Set<UserData> userDataSet = attachedUserData.get(userData.getClass());

        if (userDataSet == null) {
            userDataSet = new HashSet<>();
            attachedUserData.put(userData.getClass(), userDataSet);
        }

        if (userData instanceof SingletonUserData && userDataSet.size() > 0) {
            throw new UnsupportedOperationException("Attempted to add multiple instances of a singleton user data class.");
        }

        userDataSet.add(userData);
    }

    public void detachUserData(Class<? extends UserData> clazz) {
        Iterator<Map.Entry<Class<? extends UserData>, Set<UserData>>> iterator = attachedUserData.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Class<? extends UserData>, Set<UserData>> entry = iterator.next();

            if (clazz.isAssignableFrom(entry.getKey())) {
                iterator.remove();
            }
        }
    }

    public boolean detachUserData(UserData userData) {
        Set<UserData> userDataSet = attachedUserData.get(userData.getClass());
        return userDataSet != null && userDataSet.remove(userData);
    }

    public <T extends SingletonUserData> T getSingletonUserData(Class<T> clazz) {
        List<T> userDataList = getUserData(clazz);

        if (userDataList.size() > 1) {
            throw new IllegalStateException("Singleton user data class has multiple instances.");
        }

        return userDataList.size() > 0 ? userDataList.get(0) : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends UserData> List<T> getUserData(Class<T> clazz) {
        Set<T> userDataSet = new HashSet<>();

        for (Map.Entry<Class<? extends UserData>, Set<UserData>> entry : attachedUserData.entrySet()) {
            if (clazz.isAssignableFrom(entry.getKey())) {
                userDataSet.addAll((Set<? extends T>) entry.getValue());
            }
        }

        return Collections.unmodifiableList(new ArrayList<>(userDataSet));
    }
}
