package net.devromik.textFileIndexer;

import java.nio.file.Path;

/**
 * @author Shulnyaev Roman
 */
public interface Indexer {

    /**
     * @throws java.lang.NullPointerException если indexEventListener == null.
     */
    void addIndexEventListener(IndexEventListener indexEventListener);

    /**
     * @throws java.lang.RuntimeException если среди исходных файлов есть некорректные (см. PreconditionUtils.checkSourceFile).
     */
    void startIndexing();
    void cancelIndexing();
    IndexingStatus getIndexingStatus();

    /**
     * Возвращает индексы всех исходных файлов.
     *
     * Может быть вызван только после запуска процесса индексации через метод startIndexing().
     * При этом после запуска процесса индексации, он может быть отменен через метод cancelIndexing().
     * На работу данного метода это повлияет только в том отношении,
     * что он будет возвращать частично сформированные (но полностью работоспособные) индексы.
     *
     * @throws IndexingNotStartedException если процесс индексации ни разу не был запущен.
     */
    Iterable<Index> getAllIndexes();

    /**
     * Возвращает индекс указанного исходного файла.
     *
     * Может быть вызван только после запуска процесса индексации через метод startIndexing().
     * При этом после запуска процесса индексации, он может быть отменен через метод cancelIndexing().
     * На работу данного метода это повлияет только в том отношении,
     * что он будет возвращать частично сформированный (но полностью работоспособный) индекс.
     *
     * @throws IndexingNotStartedException если процесс индексации ни разу не был запущен.
     */
    Index getIndex(Path sourceFilePath);
}