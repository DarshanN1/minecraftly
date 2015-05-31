package com.minecraftly.core.bukkit.language;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecraftly.core.bukkit.config.ConfigWrapper;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by Keir on 20/03/2015.
 */
public class LanguageManager {

    public ConfigWrapper configWrapper;
    public FileConfiguration config;
    public Map<String, LanguageValue> languageValues = new ConcurrentHashMap<>();
    private Logger logger; // todo do we need this?

    public LanguageManager(Logger logger, File languageFile) throws IOException {
        checkNotNull(logger);
        checkNotNull(languageFile);

        this.logger = logger;
        this.configWrapper = new ConfigWrapper(languageFile);
        load();

        if (!languageFile.exists()) {
            try {
                languageFile.createNewFile();
            } catch (IOException e) {
                throw new IOException("Unable to create language file at: " + languageFile.getPath(), e);
            }
        }

        if (!languageFile.isFile()) {
            throw new IllegalArgumentException("File is not a file: " + languageFile.getPath());
        }
    }

    public void load() {
        configWrapper.reloadConfig();
        this.config = configWrapper.getConfig();

        for (Map.Entry<String, LanguageValue> entry : languageValues.entrySet()) {
            String key = entry.getKey();

            if (config.contains(key)) {
                entry.getValue().setValue(config.getString(key));
            }
        }
    }

    public Map<String, LanguageValue> getLanguageValues() {
        return Collections.unmodifiableMap(languageValues);
    }

    public void registerAll(Map<String, LanguageValue> languageValues) {
        for (Map.Entry<String, LanguageValue> entry : languageValues.entrySet()) {
            register(entry.getKey(), entry.getValue());
        }
    }

    public void register(String key, LanguageValue languageValue) {
        languageValues.put(key, languageValue);

        if (config.contains(key)) {
            languageValue.setValue(config.getString(key));
        } else {
            config.set(key, languageValue.getValue());
        }
    }

    public String getRaw(String key) {
        return languageValues.containsKey(key) ? languageValues.get(key).getValue() : null;
    }

    public String get(String key, Object... args) {
        String message = getRaw(key);
        return message != null ? String.format(message, args) : key;
    }

    public void save() {
        for (Map.Entry<String, LanguageValue> entry : languageValues.entrySet()) {
            config.set(entry.getKey(), entry.getValue().getUntouchedValue());
        }

        configWrapper.saveConfig();
    }
}
