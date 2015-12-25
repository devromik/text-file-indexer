package net.devromik.textFileIndexer.utils;

import org.slf4j.Logger;

/**
 * @author Shulnyaev Roman
 */
public final class Slf4jUtils {

    public static void logException(Logger logger, Exception exception) {
        logger.error("Exception", exception);
    }

    // ****************************** //

    private Slf4jUtils() {}
}
