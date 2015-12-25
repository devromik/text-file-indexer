package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.io.*;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.sleep;
import net.devromik.textFileIndexer.*;
import static net.devromik.textFileIndexer.IndexingStatus.*;
import net.devromik.textFileIndexer.impl.EmptyOccurrencePosIterator;
import static net.devromik.textFileIndexer.utils.PreconditionUtilsTest.makeFileMock;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class MainMemorySuffixAutomationIndexBuilderTest {

    /**
     * Проверяет, что операции построения индекса и поиска с его помощью всех вхождений заданной строки (чтение) могут выполняться параллельно.
     */
    @Test
    public void test_ReadingParallelToBuilding() throws Exception {
        // Строка, вхождения которой мы будем искать в исходном файле.
        final String SOUGHT_FOR_CHAR_SEQUENCE = "soughtForCharSequence";
        final int SOUGHT_FOR_CHAR_SEQUENCE_LENGTH = SOUGHT_FOR_CHAR_SEQUENCE.length();

        // Генерируем исходный файл:
        //     - количество символов - 10^6.
        //     - периодическая структура: строка SOUGHT_FOR_CHAR_SEQUENCE{remainder} в степени 10^2,
        //       где {remainder} - последовательность из 10^4 символов, первым элементом которой является символ 'a',
        //       а целочисленный код элемента n + 1 равен целочисленному коду элемента n плюс единица.
        // Такая структура обусловлена тем, что далее мы будем работать с понятием прогресса индексации,
        // выраженного в проценте обработанных символов исходного файла,
        // и расстановка искомой строки в началах "процентов символов" (через каждые sourceFile.length / 100 символов)
        // поможет нам получить нужную информацию.
        // {remainder} - просто заполнитель, его символьное наполнение не несет никакой смысловой нагрузки.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

        final int SOURCE_FILE_LENGTH = 1000000;
        final int PERCENT_COUNT = 100;
        final int SOUGHT_FOR_CHAR_SEQUENCE_WITH_APPENDED_REMAINDER_LENGTH = SOURCE_FILE_LENGTH / PERCENT_COUNT;

        for (int i = 0; i < PERCENT_COUNT; ++i) {
            sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
            char currentRemainderChar = 'a';

            for (int j = 0; j < SOUGHT_FOR_CHAR_SEQUENCE_WITH_APPENDED_REMAINDER_LENGTH - SOUGHT_FOR_CHAR_SEQUENCE_LENGTH; ++j) {
                sourceFileWriter.write(currentRemainderChar++);
            }
        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        final MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Получаем индекс.
        final Index index = indexBuilder.getIndex();

        // Индикатор того, что тест успешно пройден.
        final AtomicBoolean testSucceeded = new AtomicBoolean(true);

        // Потоки-читатели.
        // Потоки-читатели с четными индексами проверяют, что чтение не блокирует построение.
        // Потоки-читатели с нечетными индексами проверяют, что построение не блокирует чтение.
        final int MIN_INDEX_READING_THREAD_COUNT = 4;
        final int INDEX_READING_THREAD_COUNT = max(getRuntime().availableProcessors() - 1, MIN_INDEX_READING_THREAD_COUNT);
        Thread[] indexReadingThreads = new Thread[INDEX_READING_THREAD_COUNT];

        for (int i = 0; i < indexReadingThreads.length; ++i) {
            final int indexReadingThreadIndex = i;
            indexReadingThreads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (indexBuilder.getBuildingStatus() != INDEXING_SUCCESSFULLY_COMPLETED) {
                        try {
                            int handledCharCountBeforeReading = indexBuilder.getHandledSourceFileCharCount();
                            OccurrencePosIterator occurrencePosIter = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
                            int occurrenceCount = 0;

                            while (occurrencePosIter.hasNext()) {
                                occurrencePosIter.getNext();
                                ++occurrenceCount;

                                // Потоки-читатели с четными индексами проверяют, что чтение не блокирует построение.
                                if (indexReadingThreadIndex % 2 == 0) {
                                    int currentHandledCharCount = indexBuilder.getHandledSourceFileCharCount();

                                    // Если после получения очередного вхождения прогресс в построении индекса не достигнут,
                                    // то либо это произошло в результате неудачного тайминга (все хорошо), либо чтение заблокировало построение (тест провален).
                                    // Опуская граничные случаи, когда в рамках текущего прохода по вхождениям
                                    // мы в принципе не сможем больше наблюдать прогресс в построении индекса
                                    // (currentHandledCharCount == SOURCE_FILE_LENGTH, !occurrencePosIter.hasNext()),
                                    // следующий код проверяет, какую из этих двух ситуаций мы имеем на самом деле.
                                    if (currentHandledCharCount == handledCharCountBeforeReading) {
                                        if (currentHandledCharCount < SOURCE_FILE_LENGTH && occurrencePosIter.hasNext()) {
                                            // Даем возможность построителю индекса достигнуть прогресса.
                                            sleep(100L);
                                            currentHandledCharCount = indexBuilder.getHandledSourceFileCharCount();

                                            // Если прогресс в построении индекса все еще не достигнут, считаем, что
                                            // тест провален по причине блокировки процесса построения индекса процессом чтения индекса.
                                            if (currentHandledCharCount == handledCharCountBeforeReading) {
                                                testSucceeded.set(false);
                                            }

                                            // К этому моменту проверка завершена - выходим.
                                            return;
                                        }
                                    }
                                }
                            }

                            // Проверяем количество вхождений.
                            if (occurrenceCount < handledCharCountBeforeReading / SOUGHT_FOR_CHAR_SEQUENCE_WITH_APPENDED_REMAINDER_LENGTH) {
                                testSucceeded.set(false);
                                return;
                            }

                            // Потоки-читатели с нечетными индексами проверяют, что построение не блокирует чтение.
                            if (indexReadingThreadIndex % 2 == 1) {
                                int handledCharCountAfterReading = indexBuilder.getHandledSourceFileCharCount();

                                // Проверяем, что за одну операцию чтения обработано не более 10% символов исходного файла (построение не блокирует чтение).
                                if (handledCharCountAfterReading - handledCharCountBeforeReading >
                                    10 * SOUGHT_FOR_CHAR_SEQUENCE_WITH_APPENDED_REMAINDER_LENGTH /* 10% символов исходного файла. */) {

                                    testSucceeded.set(false);
                                    return;
                                }
                            }

                            sleep(100L);
                        }
                        catch (InterruptedException exception) {
                            testSucceeded.set(false);
                            return;
                        }
                    }
                }
            });

            indexReadingThreads[i].start();
        }

        // Ожидаем, пока потоки-читатели не завершат свою работу.
        for (Thread indexReadingThread : indexReadingThreads) {
            indexReadingThread.join();
        }

        // Проверяем, что тест успешно пройден.
        assertTrue(testSucceeded.get());

        // Завершаем процесс оптимизации индекса.
        indexBuilder.cancelBuilding();
    }

    @Test
    public void test_CancelBuilding() throws Exception {
        // Подготавливаем достаточно большой исходный файл,
        // что иметь возможность остановить процесс построения его индекса "на полпути".
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));
        final int SOURCE_FILE_LENGTH = 250000;

        for (int i = 0; i < SOURCE_FILE_LENGTH; ++i) {
            sourceFileWriter.write('a' + i);
        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();
        int handledCharCountAfterIndexBuildingCancelled = -1;

        // Предпринимаем попытки остановить процесс построения индекса на полпути до тех пор, пока нам не удастся это сделать.
        while (indexBuilder.getBuildingStatus() != INDEXING_SUCCESSFULLY_COMPLETED) {
            if (indexBuilder.getHandledSourceFileCharCount() > 0) {
                indexBuilder.cancelBuilding();

                if (indexBuilder.getHandledSourceFileCharCount() < SOURCE_FILE_LENGTH) {
                    handledCharCountAfterIndexBuildingCancelled = indexBuilder.getHandledSourceFileCharCount();
                    assertTrue(handledCharCountAfterIndexBuildingCancelled < SOURCE_FILE_LENGTH);
                    break;
                }
                else {
                    indexBuilder = makeIndexBuilder(sourceFile);
                    indexBuilder.build();
                }
            }
            else {
                sleep(10L);
            }
        }

        // Ожидаем некоторое время.
        sleep(250L);

        // Проверяем, что прогресса в построении индекса не было с момента завершения операции отмены построения индекса.
        assertThat(indexBuilder.getHandledSourceFileCharCount(), is(handledCharCountAfterIndexBuildingCancelled));
        assertThat(indexBuilder.getBuildingStatus(), is(INDEXING_NOT_STARTED));
    }

    /* ***** Предусловия. ***** */

    @Test(expected = IllegalStateException.class)
    public void test_SourceFileNotNull_Precondition() throws Exception {
        makeIndexBuilder(null).build();
    }

    @Test(expected = IllegalStateException.class)
    public void test_SourceFileIsNotDirectory_Precondition() throws Exception {
        makeIndexBuilder(makeFileMock(true, true, true)).build();
    }

    @Test(expected = IllegalStateException.class)
    public void test_SourceFileIsReadable_Precondition() throws Exception {
        makeIndexBuilder(makeFileMock(false, false, true)).build();
    }

    /* ***** Поиск всех вхождений. ***** */

    @Test
    public void test_OccurrenceSearching_When_SourceFileIsEmpty() throws Exception {
        File sourceFile = workDirectoryManager.newFile();
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();
        assertThat(indexBuilder.getIndex().getOccurrencePosIterator("abc"), instanceOf(EmptyOccurrencePosIterator.class));
    }

    @Test
    public void test_OccurrenceSearching_When_SourceFileIsCharDegree() throws Exception {
        // Подготавливаем исходный файл.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));
        final int SOURCE_FILE_LENGTH = 1000;
        final String SOUGHT_FOR_CHAR = "a";

        for (int i = 0; i < SOURCE_FILE_LENGTH; ++i) {
            sourceFileWriter.write(SOUGHT_FOR_CHAR);
        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        Index index = indexBuilder.getIndex();

        // Проверяем вхождения.

        // SOUGHT_FOR_CHAR.
        OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR);

        for (long i = 0; i < SOURCE_FILE_LENGTH; ++i) {
            assertThat(occurrencePosIterator.getNext(), is(i));
        }

        assertFalse(occurrencePosIterator.hasNext());

        // SOUGHT_FOR_CHAR^SOURCE_FILE_LENGTH.
        occurrencePosIterator = index.getOccurrencePosIterator(repeat(SOUGHT_FOR_CHAR, SOURCE_FILE_LENGTH));
        assertThat(occurrencePosIterator.getNext(), is(0L));
        assertFalse(occurrencePosIterator.hasNext());

        // "b".
        occurrencePosIterator = index.getOccurrencePosIterator("b");
        assertFalse(occurrencePosIterator.hasNext());
    }

    @Test
    public void test_OccurrenceSearching_When_SourceFileIsStringDegree() throws Exception  {
        // Подготавливаем исходный файл.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));
        final String SOUGHT_FOR_CHAR_SEQUENCE = "abc";
        final int SOUGHT_FOR_CHAR_SEQUENCE_LENGTH = SOUGHT_FOR_CHAR_SEQUENCE.length();
        final int SOUGHT_FOR_CHAR_SEQUENCE_DEGREE = 1000;

        for (int i = 0; i < SOUGHT_FOR_CHAR_SEQUENCE_DEGREE; ++i) {
            sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        Index index = indexBuilder.getIndex();

        // Проверяем вхождения.

        // SOUGHT_FOR_CHAR_SEQUENCE.
        OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);

        for (long i = 0; i < SOUGHT_FOR_CHAR_SEQUENCE_DEGREE; ++i) {
            assertThat(occurrencePosIterator.getNext(), is(i * SOUGHT_FOR_CHAR_SEQUENCE_LENGTH));
        }

        assertFalse(occurrencePosIterator.hasNext());

        // SOUGHT_FOR_CHAR_SEQUENCE^SOUGHT_FOR_CHAR_SEQUENCE_DEGREE.
        occurrencePosIterator = index.getOccurrencePosIterator(repeat(SOUGHT_FOR_CHAR_SEQUENCE, SOUGHT_FOR_CHAR_SEQUENCE_DEGREE));
        assertThat(occurrencePosIterator.getNext(), is(0L));
        assertFalse(occurrencePosIterator.hasNext());

        // "def".
        occurrencePosIterator = index.getOccurrencePosIterator("def");
        assertFalse(occurrencePosIterator.hasNext());
    }

    @Test
    public void test_OccurrenceSearching_When_SourceFileNotContainsSoughtForCharSequence() throws Exception  {
        // Подготавливаем исходный файл.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));
        final int SOURCE_FILE_LENGTH = 1000;

        for (int i = 0; i < SOURCE_FILE_LENGTH; ++i) {
            sourceFileWriter.write("z");
        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        Index index = indexBuilder.getIndex();

        // Проверяем вхождения.
        final String SOUGHT_FOR_CHAR_SEQUENCE = "abc";

        // SOUGHT_FOR_CHAR_SEQUENCE.
        OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
        assertFalse(occurrencePosIterator.hasNext());
    }

    @Test
    public void test_OccurrenceSearching_When_SourceFileContainsOneSoughtForCharSequenceOccurrence() throws Exception  {
        final String SOUGHT_FOR_CHAR_SEQUENCE = "abc";

        /* ***** Вхождение в начале исходного файла. ***** */

        // Подготавливаем исходный файл.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

        for (int i = 0; i < 1000; ++i) {
            if (i == 0) {
                sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
            }
            else {
                sourceFileWriter.write("z");
            }

        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        Index index = indexBuilder.getIndex();

        // Проверяем вхождения.
        OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
        assertThat(occurrencePosIterator.getNext(), is(0L));
        assertFalse(occurrencePosIterator.hasNext());

        /* ***** Вхождение в середине исходного файла. ***** */

        sourceFile = workDirectoryManager.newFile();
        sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

        for (int i = 0; i < 1000; ++i) {
            if (i == 500) {
                sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
            }
            else {
                sourceFileWriter.write("z");
            }

        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        index = indexBuilder.getIndex();

        // Проверяем вхождения.
        occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
        assertThat(occurrencePosIterator.getNext(), is(500L));
        assertFalse(occurrencePosIterator.hasNext());

        /* ***** Вхождение в конце исходного файла. ***** */

        sourceFile = workDirectoryManager.newFile();
        sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

        for (int i = 0; i < 1000; ++i) {
            if (i == 999) {
                sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
            }
            else {
                sourceFileWriter.write("z");
            }

        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        index = indexBuilder.getIndex();

        // Проверяем вхождения.
        occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
        assertThat(occurrencePosIterator.getNext(), is(999L));
        assertFalse(occurrencePosIterator.hasNext());
    }

    @Test
    public void test_OccurrenceSearching_When_SourceFileContainsManySoughtForCharSequenceOccurrences() throws Exception {
        final String SOUGHT_FOR_CHAR_SEQUENCE = "abc";

        // Подготавливаем исходный файл.
        File sourceFile = workDirectoryManager.newFile();
        Writer sourceFileWriter = new BufferedWriter(new FileWriter(sourceFile));

        for (int i = 0; i < 1000; ++i) {
            if (i == 0 || i == 500 || i == 999) {
                sourceFileWriter.write(SOUGHT_FOR_CHAR_SEQUENCE);
            }
            else {
                sourceFileWriter.write("z");
            }

        }

        sourceFileWriter.close();

        // Запускаем процесс построения индекса.
        MainMemorySuffixAutomationIndexBuilder indexBuilder = makeIndexBuilder(sourceFile);
        indexBuilder.build();

        // Ожидаем, пока индекс не будет полностью построен.
        waitForIndexBuildingCompleted(indexBuilder);

        // Получаем индекс.
        Index index = indexBuilder.getIndex();

        // Проверяем вхождения.
        OccurrencePosIterator occurrencePosIterator = index.getOccurrencePosIterator(SOUGHT_FOR_CHAR_SEQUENCE);
        assertThat(occurrencePosIterator.getNext(), is(0L));
        assertThat(occurrencePosIterator.getNext(), is(502L));
        assertThat(occurrencePosIterator.getNext(), is(1003L));
        assertFalse(occurrencePosIterator.hasNext());
    }

    // ****************************** //

    private MainMemorySuffixAutomationIndexBuilder makeIndexBuilder(File sourceFile) {
        return new MainMemorySuffixAutomationIndexBuilder(sourceFile, null, null, null);
    }

    private void waitForIndexBuildingCompleted(MainMemorySuffixAutomationIndexBuilder indexBuilder) throws InterruptedException {
        while (indexBuilder.getBuildingStatus() != INDEXING_SUCCESSFULLY_COMPLETED) {
            sleep(10L);
        }
    }

    // ****************************** //

    @Rule
    public TemporaryFolder workDirectoryManager = new TemporaryFolder();
}