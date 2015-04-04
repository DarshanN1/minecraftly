package com.minecraftly.core.bukkit.language;

import com.minecraftly.core.ContentOwner;
import com.minecraftly.core.bukkit.utilities.Utilities;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Keir on 20/03/2015.
 */
public class LanguageValue {

    protected String unformattedValue;
    private ContentOwner contentOwner;
    private String def;
    private String value;

    public LanguageValue(ContentOwner contentOwner, String def) {
        checkNotNull(contentOwner);
        checkNotNull(def);

        this.contentOwner = contentOwner;
        this.def = def;
        setValue(def);
    }

    public ContentOwner getContentOwner() {
        return contentOwner;
    }

    public String getDefaultValue() {
        return def;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null) {
            this.unformattedValue = getDefaultValue();
        } else {
            this.unformattedValue = value;
        }

        this.value = Utilities.translateAlternateColorCodes('&', this.unformattedValue);
    }
}
