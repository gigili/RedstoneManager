package dev.igorilic.redstonemanager.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class RMLogger {
    private static final Logger LOGGER = getLogger();

    private static final String LOG_PREFIX = "[RedstoneManager] ";

    private static Logger getLogger() {
        return LogUtils.getLogger();
    }

    public static void info(String message, Object... arguments) {
        LOGGER.info("{} {}", LOG_PREFIX, message.formatted(arguments));
    }
}
