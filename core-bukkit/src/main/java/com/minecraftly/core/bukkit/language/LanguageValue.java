package com.minecraftly.core.bukkit.language;

import com.minecraftly.core.ContentOwner;
import com.minecraftly.core.bukkit.config.DataValue;

/**
 * A {@link DataValue} where users are forced to use the {@link String} type.
 * This reduces code for language value handling.
 */
public class LanguageValue extends DataValue<String> {

    public LanguageValue(ContentOwner contentOwner, String def) {
        super(contentOwner, def, String.class);
    }

}
