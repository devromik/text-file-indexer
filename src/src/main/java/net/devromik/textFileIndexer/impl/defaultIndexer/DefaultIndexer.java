package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.*;
import static java.lang.Runtime.getRuntime;
import net.devromik.textFileIndexer.utils.*;
import static net.devromik.textFileIndexer.utils.ExceptionUtils.wrapInRuntimeException;
import static net.devromik.textFileIndexer.utils.Slf4jUtils.logException;
import net.devromik.textFileIndexer.*;
import net.devromik.textFileIndexer.impl.utils.IndexEventUtils;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Shulnyaev Roman
 */
public final class DefaultIndexer implements Indexer, IndexEventListener {

    public static final int MAX_SOURCE_FILE_COUNT = 4096;
    public static final int MIN_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT = 1;
    public static final int MAX_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT = 256;
    public static final int DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT = getRuntime().availableProcessors();

    /* ***** Реализация Indexer. ***** */

    @Override
    public void addIndexEventListener(IndexEventListener indexEventListener) {
        indexEventListeners.add(PreconditionUtils.checkNotNull(indexEventListener));
    }

    @Override
    public void startIndexing() {
        // Блокировка является "мгновенной",
        // поскольку запускается не более maxSimultaneousIndexBuildingCount асинхронных процессов индексации.
        synchronized (indexingMon) {
            if (indexingStatus == IndexingStatus.INDEXING_IN_PROGRESS || indexingStatus == IndexingStatus.INDEXING_SUCCESSFULLY_COMPLETED) {
                return;
            }

            setIndexingStatus(IndexingStatus.INDEXING_IN_PROGRESS);

            if (isFirstIndexingStart()) {
                prepareForFirstIndexingStart();
            }

            if (!indexBuildingQueue.isEmpty()) {
                int indexBuildingCount = 0;

                while (!indexBuildingQueue.isEmpty() && indexBuildingCount++ < maxSimultaneousIndexBuildingCount) {
                    Path sourceFilePath = indexBuildingQueue.pop();
                    getIndexBuilderFor(sourceFilePath).build();
                    indexBuildingDesktop.add(sourceFilePath);
                }
            }
            else {
                setIndexingStatus(IndexingStatus.INDEXING_SUCCESSFULLY_COMPLETED);
            }
        }
    }

    @Override
    public void cancelIndexing() {
        // Блокировка является "мгновенной",
        // поскольку процессы индексации отменяются за константное время
        // (реализации построителей индексов должны обеспечивать такую гарантию по своему контракту).
        synchronized (indexingMon) {
            if (indexingStatus != IndexingStatus.INDEXING_IN_PROGRESS) {
                return;
            }

            setIndexingStatus(IndexingStatus.INDEXING_CANCELLING_IN_PROGRESS);

            for (Path indexingSourceFilePath : indexBuildingDesktop) {
                cancelIndexBuilding(indexingSourceFilePath);
                indexBuildingQueue.addFirst(indexingSourceFilePath);
            }

            indexBuildingDesktop.clear();
            setIndexingStatus(IndexingStatus.INDEXING_NOT_STARTED);
        }
    }

    @Override
    public IndexingStatus getIndexingStatus() {
       synchronized (indexingMon) {
           return indexingStatus;
       }
    }

    @Override
    public Iterable<Index> getAllIndexes() {
        synchronized (indexingMon) {
            checkIndexingStarted();

            if (allIndexes == null) {
                allIndexes = new Iterable<Index>() {

                    @Override
                    public Iterator<Index> iterator() {

                        return new Iterator<Index>() {

                            @Override
                            public boolean hasNext() {
                                return sourceFileLengthAndPathIter.hasNext();
                            }

                            @Override
                            public Index next() {
                                return getIndexBuilderFor(sourceFileLengthAndPathIter.next().sourceFilePath).getIndex();
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException("Not supported");
                            }

                            // ****************************** //

                            private Iterator<SourceFileLengthAndPathOrderedPair> sourceFileLengthAndPathIter = sourceFileLengthAndPathToIndexBuilder.keySet().iterator();
                        };
                    }
                };
            }

            return allIndexes;
        }
    }

    @Override
    public Index getIndex(Path sourceFilePath) {
        synchronized (indexingMon) {
            checkIndexingStarted();
            return getIndexBuilderFor(sourceFilePath).getIndex();
        }
    }

    /* ***** Реализация IndexEventListener. ***** */

    @Override
    public void onIndexBuildingStarted(Path sourceFilePath) {
        IndexEventUtils.notifyOnIndexBuildingStarted(indexEventListeners, sourceFilePath);
    }

    @Override
    public void onIndexBuildingCancelled(Path sourceFilePath) {
        IndexEventUtils.notifyOnIndexBuildingCancelled(indexEventListeners, sourceFilePath);
    }

    @Override
    public void onIndexBuildingSuccessfullyCompleted(Path sourceFilePath) {
        synchronized (indexingMon) {
            if (indexBuildingDesktop.contains(sourceFilePath)) {
                indexBuildingDesktop.remove(sourceFilePath);

                if (!indexBuildingQueue.isEmpty()) {
                    Path nextIndexingSourceFilePath = indexBuildingQueue.pop();
                    getIndexBuilderFor(nextIndexingSourceFilePath).build();
                    indexBuildingDesktop.add(nextIndexingSourceFilePath);
                }
                else {
                    setIndexingStatus(IndexingStatus.INDEXING_SUCCESSFULLY_COMPLETED);
                }
            }
        }

        IndexEventUtils.notifyOnIndexBuildingSuccessfullyCompleted(indexEventListeners, sourceFilePath);
    }

    @Override
    public void onIndexBuildingErrorOccurred(Path sourceFilePath, Exception indexBuildingException) {
        IndexEventUtils.notifyOnIndexBuildingErrorOccurred(indexEventListeners, sourceFilePath, indexBuildingException);
    }

    // ****************************** //

    DefaultIndexer(
        Collection<? extends Path> sourceFilePaths,
        IndexBuilderChooser indexBuilderChooser,
        int maxSimultaneousIndexBuildingCount) {

        this.sourceFilePaths = sourceFilePaths;
        this.indexBuilderChooser = indexBuilderChooser;
        this.maxSimultaneousIndexBuildingCount = maxSimultaneousIndexBuildingCount;
        setIndexingStatus(IndexingStatus.INDEXING_NOT_STARTED);
    }

    IndexBuilder getIndexBuilderFor(Path sourceFilePath) {
        synchronized (indexingMon) {
            return sourceFileLengthAndPathToIndexBuilder.get(getSourceFileLengthAndPathFor(sourceFilePath));
        }
    }

    int getIndexBuildingQueueSize() {
        synchronized (indexingMon) {
            return indexBuildingQueue.size();
        }
    }

    int getIndexBuildingDesktopSize() {
        synchronized (indexingMon) {
            return indexBuildingDesktop.size();
        }
    }

    // ****************************** //

    private void setIndexingStatus(IndexingStatus indexingStatus) {
        this.indexingStatus = indexingStatus;
    }

    private void checkIndexingStarted() {
        if (indexingStatus == IndexingStatus.INDEXING_NOT_STARTED) {
            throw new IndexingNotStartedException();
        }
    }

    private boolean isFirstIndexingStart() {
        return infoOnSourceFiles == null;
    }

    private void prepareForFirstIndexingStart() {
        try {
            int sourceFileCount = sourceFilePaths.size();
            infoOnSourceFiles = new HashMap<>(sourceFileCount);
            sourceFileLengthAndPathToIndexBuilder = new TreeMap<>();

            for (Path sourceFilePath : sourceFilePaths) {
                if (!infoOnSourceFiles.containsKey(sourceFilePath)) {
                    File sourceFile = sourceFilePath.toFile();
                    PreconditionUtils.checkSourceFile(sourceFile);
                    infoOnSourceFiles.put(sourceFilePath, new SourceFileInfo(sourceFile.length()));
                    IndexBuilder indexBuilder = indexBuilderChooser.choose(sourceFilePath);
                    indexBuilder.setSourceFile(sourceFile);
                    indexBuilder.setIndexEventListener(this);
                    sourceFileLengthAndPathToIndexBuilder.put(getSourceFileLengthAndPathFor(sourceFilePath), indexBuilder);
                }
            }

            indexBuildingQueue = new ArrayDeque<>(sourceFileCount);
            indexBuildingDesktop = new HashSet<>(maxSimultaneousIndexBuildingCount);

            for (SourceFileLengthAndPathOrderedPair sourceFileLengthAndPath : sourceFileLengthAndPathToIndexBuilder.keySet()) {
                indexBuildingQueue.add(sourceFileLengthAndPath.sourceFilePath);
            }
        }
        catch (Exception exception) {
            setIndexingStatus(IndexingStatus.INDEXING_NOT_STARTED);

            infoOnSourceFiles = null;
            sourceFileLengthAndPathToIndexBuilder = null;
            indexBuildingQueue = null;
            indexBuildingDesktop = null;

            throw wrapInRuntimeException(exception);
        }
    }

    private void cancelIndexBuilding(Path indexingSourceFilePath) {
        try {
            getIndexBuilderFor(indexingSourceFilePath).cancelBuilding();
        }
        catch (Exception exception) {
            logException(logger, exception);
        }
    }

    private SourceFileLengthAndPathOrderedPair getSourceFileLengthAndPathFor(Path sourceFilePath) {
        return new
            SourceFileLengthAndPathOrderedPair(
                infoOnSourceFiles.get(sourceFilePath).getSourceFileLength(),
                sourceFilePath);
    }

    // ****************************** //

    private final Collection<? extends Path> sourceFilePaths;
    private final IndexBuilderChooser indexBuilderChooser;
    private final int maxSimultaneousIndexBuildingCount;

    private IndexingStatus indexingStatus;
    private final CopyOnWriteArrayList<IndexEventListener> indexEventListeners = new CopyOnWriteArrayList<>();

    private Map<Path, SourceFileInfo> infoOnSourceFiles;
    private NavigableMap<SourceFileLengthAndPathOrderedPair, IndexBuilder> sourceFileLengthAndPathToIndexBuilder;
    private Deque<Path> indexBuildingQueue;

    // Содержит пути тех и только тех исходных файлов, индексация которых выполняется в настоящий момент.
    private Set<Path> indexBuildingDesktop;

    private Iterable<Index> allIndexes;
    private final Object indexingMon = new Object();

    // Логгер.
    private static final Logger logger = getLogger(DefaultIndexer.class);
}
