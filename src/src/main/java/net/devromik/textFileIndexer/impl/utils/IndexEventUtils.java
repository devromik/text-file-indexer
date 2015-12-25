package net.devromik.textFileIndexer.impl.utils;

import java.nio.file.Path;
import java.util.Collection;
import org.slf4j.*;
import net.devromik.textFileIndexer.IndexEventListener;
import static net.devromik.textFileIndexer.utils.PreconditionUtils.checkNotNull;
import static net.devromik.textFileIndexer.utils.Slf4jUtils.logException;
import static org.slf4j.LoggerFactory.*;

/**
 * @author Shulnyaev Roman
 */
public final class IndexEventUtils {

    public static void notifyOnIndexBuildingStarted(Collection<? extends IndexEventListener> listeners, Path sourceFilePath) {
        checkNotNull(listeners);
        checkNotNull(sourceFilePath);

        for (IndexEventListener listener : listeners) {
            notifyOnIndexBuildingStarted(listener, sourceFilePath);
        }
    }

    public static void notifyOnIndexBuildingStarted(IndexEventListener listener, Path sourceFilePath) {
        checkNotNull(sourceFilePath);

        try {
            if (listener != null) {
                listener.onIndexBuildingStarted(sourceFilePath);
            }
        }
        catch (Exception exception) {
            logException(logger, exception);
        }
    }

    // ****************************** //

    public static void notifyOnIndexBuildingCancelled(Collection<? extends IndexEventListener> listeners, Path sourceFilePath) {
        checkNotNull(listeners);
        checkNotNull(sourceFilePath);

        for (IndexEventListener listener : listeners) {
            notifyOnIndexBuildingCancelled(listener, sourceFilePath);
        }
    }

    public static void notifyOnIndexBuildingCancelled(IndexEventListener listener, Path sourceFilePath) {
        checkNotNull(sourceFilePath);

        try {
            if (listener != null) {
                listener.onIndexBuildingCancelled(sourceFilePath);
            }
        }
        catch (Exception exception) {
            logException(logger, exception);
        }
    }

    // ****************************** //

    public static void notifyOnIndexBuildingSuccessfullyCompleted(Collection<? extends IndexEventListener> listeners, Path sourceFilePath) {
        checkNotNull(listeners);
        checkNotNull(sourceFilePath);

        for (IndexEventListener listener : listeners) {
            notifyOnIndexBuildingSuccessfullyCompleted(listener, sourceFilePath);
        }
    }

    public static void notifyOnIndexBuildingSuccessfullyCompleted(IndexEventListener listener, Path sourceFilePath) {
        checkNotNull(sourceFilePath);

        try {
            if (listener != null) {
                listener.onIndexBuildingSuccessfullyCompleted(sourceFilePath);
            }
        }
        catch (Exception exception) {
            logException(logger, exception);
        }
    }

    // ****************************** //

    public static void notifyOnIndexBuildingErrorOccurred(
        Collection<? extends IndexEventListener> listeners,
        Path sourceFilePath,
        Exception indexBuildingException) {

        checkNotNull(listeners);
        checkNotNull(sourceFilePath);
        checkNotNull(indexBuildingException);

        for (IndexEventListener listener : listeners) {
            notifyOnIndexBuildingErrorOccurred(listener, sourceFilePath, indexBuildingException);
        }
    }

    public static void notifyOnIndexBuildingErrorOccurred(
        IndexEventListener listener,
        Path sourceFilePath,
        Exception indexBuildingException) {

        checkNotNull(sourceFilePath);
        checkNotNull(indexBuildingException);

        try {
            if (listener != null) {
                listener.onIndexBuildingErrorOccurred(sourceFilePath, indexBuildingException);
            }
        }
        catch (Exception exception) {
            logException(logger, exception);
        }
    }

    // ****************************** //

    private IndexEventUtils() {}

    // ****************************** //

    private static final Logger logger = getLogger(IndexEventUtils.class);
}
