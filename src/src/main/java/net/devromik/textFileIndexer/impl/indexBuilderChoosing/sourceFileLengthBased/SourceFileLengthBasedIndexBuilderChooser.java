package net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import net.devromik.textFileIndexer.*;
import static net.devromik.textFileIndexer.utils.ExceptionUtils.wrapInRuntimeException;
import static net.devromik.textFileIndexer.utils.PreconditionUtils.*;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;

/**
 * Схема использования:
 *     1) Формируем соответствие между размерами файлов и построителями индексов следующим образом:
 *            whenSourceFileLengthLessThan(threshold).
 *                thenChoose(indexBuilderClass).
 *                    (usingDefaultConstructor() | usingConstructorParams(indexBuilderConstructorParams))
 *            ...
 *            whenSourceFileLengthLessThan(threshold).
 *                thenChoose(indexBuilderClass).
 *                    (usingDefaultConstructor() | usingConstructorParams(indexBuilderConstructorParams)).
 *     2) Выбираем построитель индексов с помощью choose(sourceFile).
 *
 * @author Shulnyaev Roman
 */
public final class SourceFileLengthBasedIndexBuilderChooser implements IndexBuilderChooser {

    public static final long MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD = 100L;

    // ****************************** //

    /**
     * @throws java.lang.IllegalArgumentException если threshold < MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD.
     */
    public LessThanThresholdIndexBuilderPutter whenSourceFileLengthLessThan(long threshold) {
        checkLessThanThreshold(threshold);
        return new LessThanThresholdIndexBuilderPutter(this, threshold);
    }

    /**
     * Создает и возвращает построитель индексов,
     * соответствующий зарегистрированному диапазону размеров исходных файлов,
     * в который попадает sourceFileLength = sourceFilePath.toFile().length().
     *
     * Поскольку диапазоны размеров исходных файлов определяются порогами "меньше",
     * то в случае, когда sourceFileLength не меньше максимального из таких порогов,
     * подходящий построитель индексов, очевидно, не может быть выбран прямо.
     * В этом случае выбирается тот построитель индексов,
     * который соответствует максимальному из порогов "меньше".
     *
     * @throws java.lang.RuntimeException если исходный файл не является корректным (см. PreconditionUtils.checkSourceFile).
     * @throws java.lang.IllegalStateException если соответствие между размерами исходных файлов и построителями индексов пусто.
     * @throws java.lang.RuntimeException если произошла ошибка во время непосредственного конструирования построителя индексов.
     */
    @Override
    public IndexBuilder choose(Path sourceFilePath) {
        File sourceFile = sourceFilePath.toFile();
        checkSourceFile(sourceFile);
        checkState(!lessThanThresholdToIndexBuilderCreationInfo.isEmpty());

        long sourceFileLength = sourceFile.length();
        Map.Entry<Long, IndexBuilderCreationInfo> sourceFileLengthCeilingEntry =
            lessThanThresholdToIndexBuilderCreationInfo.higherEntry(
                sourceFileLength);
        IndexBuilderCreationInfo indexBuilderCreationInfo =
            sourceFileLengthCeilingEntry != null ?
            sourceFileLengthCeilingEntry.getValue() :
            lessThanThresholdToIndexBuilderCreationInfo.lastEntry().getValue();

        try {
            return
                invokeConstructor(
                    indexBuilderCreationInfo.getIndexBuilderClass(),
                    indexBuilderCreationInfo.getIndexBuilderConstructorParams());
        }
        catch (Exception exception) {
            throw wrapInRuntimeException(exception);
        }
    }

    // ****************************** //

    /**
     * @throws java.lang.IllegalArgumentException если threshold < MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD.
     */
    static void checkLessThanThreshold(long threshold) {
        checkArgument(threshold >= MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD);
    }

    /**
     * @throws java.lang.IllegalArgumentException если threshold < MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD.
     * @throws java.lang.NullPointerException если indexBuilderCreationInfo == null.
     */
    void putIndexBuilderCreationInfo(long threshold, IndexBuilderCreationInfo indexBuilderCreationInfo) {
        checkLessThanThreshold(threshold);
        checkNotNull(indexBuilderCreationInfo);

        lessThanThresholdToIndexBuilderCreationInfo.put(threshold, indexBuilderCreationInfo);
    }

    // ****************************** //

    private NavigableMap<Long, IndexBuilderCreationInfo> lessThanThresholdToIndexBuilderCreationInfo = new TreeMap<>();
}