package com.minecraftly.bukkit.user.modularisation;

import com.minecraftly.bukkit.utilities.ExceptionalConsumer;
import com.minecraftly.bukkit.user.User;
import org.apache.commons.dbutils.QueryRunner;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Used when multiple pieces of singleton user data needs to be stored.
 */
public abstract class ContainerUserData<K, V extends UserData> extends SingletonUserData {

    private Map<K, V> keyDataMap = new HashMap<>();

    public ContainerUserData(User user, Supplier<QueryRunner> queryRunnerSupplier) {
        super(user, queryRunnerSupplier);
    }

    public V getOrLoad(K key) {
        return getOrLoad(key, true);
    }

    public V getOrLoad(K key, boolean createIfNotExists) {
        V value = get(key);

        if (value == null) {
            value = load(key, createIfNotExists);
        }

        return value;
    }

    public V get(K key) {
        return keyDataMap.get(key);
    }

    public void put(K key, V value) {
        keyDataMap.put(key, value);
    }

    public void remove(K key) {
        keyDataMap.remove(key);
    }

    protected abstract V load(K key, boolean createIfNotExists);

    // helper method
    private void forEachHandleException(Collection<V> collection, ExceptionalConsumer<V> consumer) {
        for (V v : collection) {
            try {
                consumer.accept(v);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    @Override
    public void extractFrom(Player player) {
        forEachHandleException(keyDataMap.values(), v -> v.extractFrom(player));
    }

    @Override
    public void load() throws SQLException {
        forEachHandleException(keyDataMap.values(), UserData::load);
    }

    @Override
    public void apply(Player player) {
        forEachHandleException(keyDataMap.values(), v -> v.apply(player));
    }

    @Override
    public void save() throws SQLException {
        forEachHandleException(keyDataMap.values(), UserData::save);
    }
}
