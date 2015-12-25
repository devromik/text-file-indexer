package net.devromik.textFileIndexer.utils;

import java.io.File;

/**
 * @author Shulnyaev Roman
 */
public final class PreconditionUtils {

    /**
     * @throws java.lang.NullPointerException если referenceUnderTest == null.
     */
    public static <T> T checkNotNull(T referenceUnderTest) {
        if (referenceUnderTest == null) {
            throw new NullPointerException();
        }

        return referenceUnderTest;
    }

    /**
     * @throws java.lang.IllegalArgumentException если argPrecondition == false.
     */
    public static void checkArgument(boolean argPrecondition) {
        if (!argPrecondition) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @throws java.lang.IllegalArgumentException если longUnderTest < 0.
     */
    public static long checkNonNegative(long longUnderTest) {
        checkArgument(longUnderTest >= 0);
        return longUnderTest;
    }

    /**
     * @throws java.lang.IllegalStateException если statePrecondition == false.
     */
    public static void checkState(boolean statePrecondition) {
        if (!statePrecondition) {
            throw new IllegalStateException();
        }
    }

    /**
     * Исходный файл (индексируемый файл) sourceFile считается корректным тогда и только тогда,
     * когда выполнены следующие условия:
     *     - sourceFile != null.
     *     - sourceFile существует.
     *     - sourceFile не является директорией.
     *     - sourceFile доступен для чтения.
     *
     * @throws java.lang.RuntimeException если исходный файл не является корректным.
     */
    public static void checkSourceFile(File sourceFile) {
        checkNotNull(sourceFile);
        checkArgument(!sourceFile.isDirectory());
        checkArgument(sourceFile.canRead());
    }

    /**
     * Целевая директория (директория с индексными файлами) targetDirectory считается корректной тогда и только тогда,
     * когда выполнены следующие условия:
     *     - targetDirectory != null.
     *     - targetDirectory существует.
     *     - targetDirectory является директорией.
     *     - targetDirectory доступна для чтения.
     *     - targetDirectory доступна для записи.
     *
     * @throws java.lang.RuntimeException если целевая директория не является корректной.
     */
    public static void checkTargetDirectory(File targetDirectory) {
        checkNotNull(targetDirectory);
        checkArgument(targetDirectory.isDirectory());
        checkArgument(targetDirectory.canRead());
        checkArgument(targetDirectory.canWrite());
    }

    // ****************************** //

    private PreconditionUtils() {}
}