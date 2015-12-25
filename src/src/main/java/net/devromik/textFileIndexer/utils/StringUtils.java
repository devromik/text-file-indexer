package net.devromik.textFileIndexer.utils;

/**
 * @author Shulnyaev Roman
 */
public final class StringUtils {

    public static boolean isNullOrEmpty(String stringUnderTest) {
        return stringUnderTest == null || stringUnderTest.isEmpty();
    }

    // ****************************** //

    private StringUtils() {}
}
