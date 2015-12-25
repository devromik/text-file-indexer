package net.devromik.textFileIndexer.utils;

/**
 * @author Shulnyaev Roman
 */
public final class ExceptionUtils {

    public static RuntimeException wrapInRuntimeException(Exception exception) {
        PreconditionUtils.checkNotNull(exception);
        return new RuntimeException(exception);
    }

    public static IllegalStateException wrapInIllegalStateException(Exception exception) {
        PreconditionUtils.checkNotNull(exception);
        return new IllegalStateException(exception);
    }
    
    // ****************************** //

    private ExceptionUtils() {}
}
