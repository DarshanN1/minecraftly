package com.minecraftly.core.bukkit.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles a configuration file whereby option come from different locations and we are required to track them.
 */
public class ConfigManager {

    private ConfigWrapper configWrapper;
    private Map<String, DataValue> values = new HashMap<>();

    public ConfigManager(File configFile) {
        this.configWrapper = new ConfigWrapper(configFile);
        load();
    }

    public void load() {
        configWrapper.reloadConfig();
        loadSection("", configWrapper.getConfig());
    }

    private void loadSection(String keyPrefix, ConfigurationSection configurationSection) {
        if (!keyPrefix.equals("") && !keyPrefix.endsWith(".")) {
            keyPrefix += ".";
        }

        for (Map.Entry<String, Object> entry : configurationSection.getValues(true).entrySet()) {
            String path = keyPrefix + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                loadSection(path, (ConfigurationSection) value);
            } else {
                DataValue<Object> dataValue = getAnonymousValue(path);

                if (dataValue != null) {
                    dataValue.setValue(value);
                }
            }
        }
    }

    public void register(String key, DataValue value) throws ClassCastException {
        checkNotNull(key, "Key must not be null.");
        checkNotNull(value, "Value must not be null.");

        if (value.getHandler() != null) {
            if (value.getHandler() != this) {
                throw new UnsupportedOperationException("Data value (" + key + ") cannot be handled by more then 1 config manager.");
            }
        } else {
            Object obj = configWrapper.getConfig().get(key);
            if (obj != null) {
                value.setValue(value.getTypeClass().cast(obj));
            }

            values.put(key, value);
            value.setHandler(this);
        }
    }

    public void registerAll(Map<String, DataValue> values) {
        for (Map.Entry<String, DataValue> entry : values.entrySet()) {
            try {
                register(entry.getKey(), entry.getValue());
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, DataValue> getValues() {
        return Collections.unmodifiableMap(values);
    }

    @SuppressWarnings("unchecked")
    public DataValue<Object> getAnonymousValue(String key) {
        return values.get(key);
    }

    /**
     * Fetches the {@link DataValue} of a key and casts it to the defined data value type.
     *
     * @param key the key to fetch
     * @param type the class of the type to cast the data value to
     * @param <T> the type the data value should be cast to
     * @return the data value of the key, cast to the desired data value type
     * @throws ClassCastException thrown if the value of the key cannot be cast to the desired data value type
     */
    @SuppressWarnings("unchecked")
    public <T> DataValue<T> getDataValue(String key, Class<T> type) throws ClassCastException {
        try {
            return (DataValue<T>) getAnonymousValue(key);
        } catch (ClassCastException e) {
            throw new ClassCastException("Value of '" + key + "' cannot be cast to " + type.getName() + ".");
        }
    }

    /**
     * Fetches the value of a key and casts it to the defined type.
     *
     * @param key the key to fetch
     * @param type the class of the type to cast the value to
     * @param <T> the type the value should be cast to
     * @return the value of the key, cast to the desired type
     * @throws ClassCastException thrown if the value of the key cannot be cast to the desired type
     */
    public <T> T getValue(String key, Class<T> type) throws ClassCastException {
        return getDataValue(key, type).getValue();
    }

    /**
     * Fetches the value of a key in {@link String} form.
     *
     * @param key the key to fetch
     * @return the value of the key in string form
     * @throws ClassCastException thrown if the value of the key cannot be cast to a string
     */
    public String getStringValue(String key) throws ClassCastException {
        return getValue(key, String.class);
    }

    public int getIntValue(String key) throws ClassCastException {
        return getValue(key, Integer.class);
    }

    public long getLongValue(String key) throws ClassCastException {
        return getValue(key, Long.class);
    }

    public List<?> getListValue(String key) throws ClassCastException {
        return getValue(key, List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringListValue(String key) throws ClassCastException {
        return (List<String>) getListValue(key);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntListValue(String key) throws ClassCastException {
        return (List<Integer>) getListValue(key);
    }

    public void save() {
        FileConfiguration configuration = configWrapper.getConfig();

        for (Map.Entry<String, DataValue> entry : values.entrySet()) {
            configuration.set(entry.getKey(), entry.getValue().getUntouchedValue());
        }

        configWrapper.saveConfig();
    }

}
