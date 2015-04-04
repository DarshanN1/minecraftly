package com.minecraftly.core.bukkit.utilities;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A modified {@link Logger} which prepends all messages with a prefix
 *
 * @see java.util.logging.Logger
 */
public class PrefixedLogger extends Logger {

    private String prefix;

    public PrefixedLogger(String name, String prefix, Logger parent) {
        super(name, null);
        this.prefix = prefix;
        setParent(parent);
        setLevel(Level.ALL);
    }

    /**
     * Log a LogRecord.
     * <p/>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    @Override
    public void log(LogRecord record) {
        record.setMessage(prefix + record.getMessage());
        super.log(record);
    }
}
