package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.sleep;
import static java.util.Collections.sort;
import static java.util.concurrent.Executors.*;
import net.devromik.textFileIndexer.impl.*;
import static net.devromik.textFileIndexer.impl.defaultIndexer.DefaultIndexer.DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT;
import net.devromik.textFileIndexer.impl.suffixAutomation.*;
import static net.devromik.textFileIndexer.impl.suffixAutomation.MainMemorySuffixAutomationIndex.MAX_ACCEPTABLE_SOURCE_FILE_LENGTH;
import static net.devromik.textFileIndexer.impl.utils.IndexEventUtils.*;
import static net.devromik.textFileIndexer.utils.PreconditionUtilsTest.makeFileMock;
import net.devromik.textFileIndexer.*;
import net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased.SourceFileLengthBasedIndexBuilderChooser;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class DefaultIndexerTest {

    @Test
    public void test_GeneralIndexing() throws Exception {
        // Подготавливаем исходные файлы.
        // Все они будут иметь одно и то же содержимое - строку "abcde" в степени 1000.
        final int SOURCE_FILE_COUNT = 128;
        Collection<Path> sourceFilePaths = new ArrayList<>(SOURCE_FILE_COUNT);

        for (int i = 0; i < SOURCE_FILE_COUNT; ++i) {
            File sourceFile = workDirectoryManager.newFile();
            Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

            for (int j = 0; j < 1000; ++j) {
                sourceFileWriter.write("abcde");
            }

            sourceFileWriter.close();
            sourceFilePaths.add(sourceFile.toPath());
        }

        // Создаем индексатор.
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(MainMemorySuffixAutomationIndexBuilder.class).
                    usingDefaultConstructor();
        Indexer indexer = new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).buildIndexer();

        // Запускам процесс индексации.
        indexer.startIndexing();

        // Ожидаем, пока процесс индексации не будет завершен.
        waitForIndexingCompleted(indexer);

        for (Index index : indexer.getAllIndexes()) {
            assertThat(sourceFilePaths.isEmpty(), is(false));

            // Проверяем правильность и количество найденных индексатором позиций вхождений.
            OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator("abcde");
            int occurrenceCount = 0;

            while (occurrencePosIterator.hasNext()) {
                assertThat(occurrencePosIterator.getNext(), is(occurrenceCount * (long)"abcde".length()));
                ++occurrenceCount;
            }

            assertThat(occurrenceCount, is(1000));

            // Помечаем текущий индекс как проверенный.
            sourceFilePaths.remove(index.getSourceFilePath());
        }

        // Проверяем, что были проверены индексы всех исходных файлов.
        assertThat(sourceFilePaths.isEmpty(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void test_SourceFilesCorrectnessChecking() throws Exception {
        File incorrectSourceFile = makeFileMock(true /* true - файл является директорией. */, true, true);
        Collection<Path> sourceFilePaths = new ArrayList<>();
        sourceFilePaths.add(incorrectSourceFile.toPath());

        // Создаем индексатор.
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(MainMemorySuffixAutomationIndexBuilder.class).
                    usingDefaultConstructor();
        Indexer indexer = new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).buildIndexer();

        // Запускам процесс индексации.
        indexer.startIndexing();
    }

    @Test
    public void test_RepeatedSourceFilesIgnoring() throws Exception {
        // Подготавливаем исходные файлы.
        final int SOURCE_FILE_COUNT = 128;
        List<Path> sourceFilePaths = new ArrayList<>(SOURCE_FILE_COUNT);

        for (int i = 0; i < SOURCE_FILE_COUNT; ++i) {
            sourceFilePaths.add(i % 2 == 0 ? workDirectoryManager.newFile().toPath() : sourceFilePaths.get(i - 1));
        }

        // Создаем индексатор.
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(MainMemorySuffixAutomationIndexBuilder.class).
                    usingDefaultConstructor();
        Indexer indexer = new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).buildIndexer();

        // Запускам процесс индексации.
        indexer.startIndexing();

        // Ожидаем, пока процесс индексации не будет завершен.
        waitForIndexingCompleted(indexer);

        // Создаем множество (элементы уникальны) путей исходных файлов.
        Set<Path> sourceFilePathSet = new HashSet<>(sourceFilePaths);
        assertThat(sourceFilePathSet.size(), is(SOURCE_FILE_COUNT / 2));

        // Проверяем, что дубликаты исходных файлов были проигнорированы индексатором.
        for (Index index : indexer.getAllIndexes()) {
            assertThat(sourceFilePathSet.isEmpty(), is(false));
            sourceFilePathSet.remove(index.getSourceFilePath());
        }

        assertThat(sourceFilePathSet.isEmpty(), is(true));
    }

    @Test
    public void test_IndexingExecutionInNonDecreasingOrderOfSourceFileLengths() throws Exception {
        // Подготавливаем исходные файлы.
        final int SOURCE_FILE_COUNT = 128;
        List<Path> sourceFilePaths = new ArrayList<>(SOURCE_FILE_COUNT);

        for (int i = 0; i < SOURCE_FILE_COUNT; ++i) {
            File sourceFile = workDirectoryManager.newFile();
            Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

            for (int j = 0; j < (int)(Math.random() * SOURCE_FILE_COUNT); ++j) {
                sourceFileWriter.write("a");
            }

            sourceFileWriter.close();
            sourceFilePaths.add(sourceFile.toPath());
        }

        // Создаем индексатор.
        SimultaneousIndexBuildingCounter simultaneousIndexBuildingCounter = new SimultaneousIndexBuildingCounter();
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(IndexBuilderMock.class).
                    usingConstructorParams(simultaneousIndexBuildingCounter, 1L /* Длительность построения индекса. */);
        Indexer indexer = new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).buildIndexer();

        // Запускам процесс индексации.
        indexer.startIndexing();

        // Ожидаем, пока процесс индексации не будет завершен.
        waitForIndexingCompleted(indexer);

        // Проверяем, что файлы, имеющие меньший размер, были проиндексированы первыми.
        sort(
            sourceFilePaths,
            new Comparator<Path>() {

                 @Override
                 public int compare(Path left, Path right) {
                     return Long.compare(left.toFile().length(), right.toFile().length());
                 }
             });

        for (Index index : indexer.getAllIndexes()) {
            assertThat(sourceFilePaths.remove(0).toFile().length(), equalTo(index.getSourceFilePath().toFile().length()));
        }

        assertThat(sourceFilePaths.isEmpty(), is(true));
    }

    @Test
    public void test_MaxSimultaneousIndexBuildingCountIsNotExceeded() throws Exception {
        // Подготавливаем исходные файлы.
        final int SOURCE_FILE_COUNT = 128;
        List<Path> sourceFilePaths = new ArrayList<>(SOURCE_FILE_COUNT);

        for (int i = 0; i < SOURCE_FILE_COUNT; ++i) {
            File sourceFile = workDirectoryManager.newFile();
            sourceFilePaths.add(sourceFile.toPath());
        }

        // Создаем индексатор.
        SimultaneousIndexBuildingCounter simultaneousIndexBuildingCounter = new SimultaneousIndexBuildingCounter();
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(IndexBuilderMock.class).
                    usingConstructorParams(simultaneousIndexBuildingCounter, 250L /* Длительность построения индекса. */);
        Indexer indexer =
            new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).
                    usingMaxSimultaneousIndexBuildingCount(getRuntime().availableProcessors()).buildIndexer();

        // Запускам процесс индексации.
        indexer.startIndexing();

        // Ожидаем, пока процесс индексации не будет завершен.
        waitForIndexingCompleted(indexer);

        // Проверяем, что в ходе индексации максимальное количество работающих одновременно процессов индексации было равно указанному.
        assertThat(simultaneousIndexBuildingCounter.getMaxSimultaneousIndexBuildingCount(), is(getRuntime().availableProcessors()));
    }

    @Test
    public void test_IndexingCancelling() throws Exception {
        // Подготавливаем исходные файлы.
        final int SOURCE_FILE_COUNT = 128;
        List<Path> sourceFilePaths = new ArrayList<>(SOURCE_FILE_COUNT);

        for (int i = 0; i < SOURCE_FILE_COUNT; ++i) {
            File sourceFile = workDirectoryManager.newFile();
            sourceFilePaths.add(sourceFile.toPath());
        }

        // Создаем индексатор.
        SimultaneousIndexBuildingCounter simultaneousIndexBuildingCounter = new SimultaneousIndexBuildingCounter();
        SourceFileLengthBasedIndexBuilderChooser indexBuilderChooser = new SourceFileLengthBasedIndexBuilderChooser();
        indexBuilderChooser.
            whenSourceFileLengthLessThan(MAX_ACCEPTABLE_SOURCE_FILE_LENGTH).
                thenChoose(IndexBuilderMock.class).
                    usingConstructorParams(
                        simultaneousIndexBuildingCounter,
                        100000L /* Длительность построения индекса. */);
        DefaultIndexer indexer =
            new DefaultIndexerBuilder(sourceFilePaths, indexBuilderChooser).
                    usingMaxSimultaneousIndexBuildingCount(DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT).buildIndexer();

        // Запускам процесс индексации.
        assertThat(indexer.getIndexingStatus(), CoreMatchers.is(IndexingStatus.INDEXING_NOT_STARTED));
        indexer.startIndexing();
        assertThat(indexer.getIndexingStatus(), CoreMatchers.is(IndexingStatus.INDEXING_IN_PROGRESS));
        int indexBuildingInProgressCount = 0;

        for (Path sourceFilePath : sourceFilePaths) {
            assertTrue(
                indexer.getIndexBuilderFor(sourceFilePath).getBuildingStatus() == IndexingStatus.INDEXING_NOT_STARTED ||
                indexer.getIndexBuilderFor(sourceFilePath).getBuildingStatus() == IndexingStatus.INDEXING_IN_PROGRESS);

            if (indexer.getIndexBuilderFor(sourceFilePath).getBuildingStatus() == IndexingStatus.INDEXING_IN_PROGRESS) {
                ++indexBuildingInProgressCount;
            }
        }

        assertThat(indexBuildingInProgressCount, is(DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT));

        // Отменяем процесс индексации.
        indexer.cancelIndexing();

        // Проверяем, что процесс индексации успешно отменен.
        assertThat(indexer.getIndexingStatus(), CoreMatchers.is(IndexingStatus.INDEXING_NOT_STARTED));
        sleep(100L);

        for (Path sourceFilePath : sourceFilePaths) {
            assertThat(indexer.getIndexBuilderFor(sourceFilePath).getBuildingStatus(), CoreMatchers.is(IndexingStatus.INDEXING_NOT_STARTED));
        }

        assertThat(indexer.getIndexBuildingQueueSize(), is(SOURCE_FILE_COUNT));
        assertThat(indexer.getIndexBuildingDesktopSize(), is(0));
    }

    // ****************************** //

    public static class SimultaneousIndexBuildingCounter {

        synchronized void onIndexBuildingStarted() {
            ++count;

            if (count > maxCount) {
                maxCount = count;
            }
        }

        /**
         * Построение индекса успешно завершено или отменено.
         */
        synchronized void onIndexBuildingStopped() {
            --count;
        }

        synchronized int getMaxSimultaneousIndexBuildingCount() {
            return maxCount;
        }

        // ****************************** //

        private int count;
        private int maxCount;
    }

    public static class IndexBuilderMock extends AbstractIndexBuilder {

        public IndexBuilderMock(SimultaneousIndexBuildingCounter simultaneousIndexBuildingCounter, long indexBuildingDuration) {
            this.simultaneousIndexBuildingCounter = simultaneousIndexBuildingCounter;
            this.indexBuildingDuration = indexBuildingDuration;
        }

        @Override
        public synchronized void build() {
            setBuildingStatus(IndexingStatus.INDEXING_IN_PROGRESS);
            simultaneousIndexBuildingCounter.onIndexBuildingStarted();
            executorService = newSingleThreadExecutor();
            executorService.submit(
                new Callable<Object>() {

                   @Override
                   public Object call() throws Exception {
                       notifyOnIndexBuildingStarted(getIndexEventListener(), getSourceFilePath());

                       try {
                           sleep(indexBuildingDuration);
                       }
                       catch (InterruptedException exception) {
                           notifyOnIndexBuildingErrorOccurred(getIndexEventListener(), getSourceFilePath(), exception);
                       }
                       finally {
                           setBuildingStatus(IndexingStatus.INDEXING_SUCCESSFULLY_COMPLETED);
                           simultaneousIndexBuildingCounter.onIndexBuildingStopped();
                           notifyOnIndexBuildingSuccessfullyCompleted(getIndexEventListener(), getSourceFilePath());
                       }

                       return null;
                   }
               });
        }

        @Override
        public synchronized void cancelBuilding() {
            setBuildingStatus(IndexingStatus.INDEXING_CANCELLING_IN_PROGRESS);
            executorService.shutdown();
            setBuildingStatus(IndexingStatus.INDEXING_NOT_STARTED);
            simultaneousIndexBuildingCounter.onIndexBuildingStopped();
            notifyOnIndexBuildingCancelled(getIndexEventListener(), getSourceFilePath());
        }

        @Override
        public Index getIndex() {
            return new AbstractIndex(getSourceFilePath()) {

                @Override
                public OccurrencePosIterator getOccurrencePosIterator(String soughtForCharSeq) {
                    return EmptyOccurrencePosIterator.INSTANCE;
                }
            };
        }

        // ****************************** //

        private final long indexBuildingDuration;
        private final SimultaneousIndexBuildingCounter simultaneousIndexBuildingCounter;
        private ExecutorService executorService;
    }

    // ****************************** //

    private void waitForIndexingCompleted(Indexer indexer) throws Exception {
        while (indexer.getIndexingStatus() != IndexingStatus.INDEXING_SUCCESSFULLY_COMPLETED) {
            sleep(10L);
        }
    }

    // ****************************** //

    @Rule
    public TemporaryFolder workDirectoryManager = new TemporaryFolder();
}