package com.minecraftly.core.bukkit.language;

import java.util.Map;

/**
 * Created by Keir on 25/03/2015.
 */
public interface LanguageManager {
    void reload();

    Map<String, LanguageValue> getLanguageValues();

    void registerAll(Map<String, LanguageValue> languageValues);

    void register(String key, LanguageValue languageValue);

    String getRaw(String key);

    String get(String key, Object... args);

    void save();
}
