package com.minecraftly.core.bukkit.language;

import com.minecraftly.core.bukkit.config.DataValue;

import java.util.Map;

/**
 * Created by Keir on 25/03/2015.
 */
public interface LanguageManager {
    void reload();

    Map<String, DataValue<String>> getLanguageValues();

    void registerAll(Map<String, DataValue<String>> languageValues);

    void register(String key, DataValue<String> dataValue);

    String getRaw(String key);

    String get(String key, Object... args);

    void save();
}
