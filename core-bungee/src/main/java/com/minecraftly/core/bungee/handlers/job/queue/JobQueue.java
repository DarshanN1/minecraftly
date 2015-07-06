package com.minecraftly.core.bungee.handlers.job.queue;

import com.google.common.base.Preconditions;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Created by Keir on 28/06/2015.
 */
public abstract class JobQueue<T> {

    private final Class<T> parameterType;
    private final Map<UUID, List<BiConsumer<ProxiedPlayer, T>>> jobs = new HashMap<>();

    public JobQueue(Class<T> parameterType) {
        this.parameterType = Preconditions.checkNotNull(parameterType, "Parameter type cannot be null.");
    }

    public Class<T> getParameterType() {
        return parameterType;
    }

    public boolean hasParameter() {
        return parameterType != null;
    }

    public void addJob(ProxiedPlayer proxiedPlayer, BiConsumer<ProxiedPlayer, T> consumer) {
        addJob(proxiedPlayer.getUniqueId(), consumer);
    }

    public void addJob(UUID playerUUID, BiConsumer<ProxiedPlayer, T> consumer) {
        List<BiConsumer<ProxiedPlayer, T>> list = jobs.get(playerUUID);

        if (list == null) {
            list = new ArrayList<>();
            jobs.put(playerUUID, list);
        }

        list.add(consumer);
    }

    public List<BiConsumer<ProxiedPlayer, T>> getJobs(ProxiedPlayer proxiedPlayer) {
        return getJobs(proxiedPlayer.getUniqueId());
    }

    public List<BiConsumer<ProxiedPlayer, T>> getJobs(UUID playerUUID) {
        return jobs.containsKey(playerUUID) ? jobs.get(playerUUID) : Collections.emptyList();
    }

    public void executeJobs(ProxiedPlayer proxiedPlayer, T data) {
        UUID playerUUID = proxiedPlayer.getUniqueId();
        List<BiConsumer<ProxiedPlayer, T>> list = jobs.get(playerUUID);

        if (list != null) {
            for (BiConsumer<ProxiedPlayer, T> consumer : list) {
                try {
                    consumer.accept(proxiedPlayer, data);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            jobs.remove(playerUUID);
        }
    }

}
