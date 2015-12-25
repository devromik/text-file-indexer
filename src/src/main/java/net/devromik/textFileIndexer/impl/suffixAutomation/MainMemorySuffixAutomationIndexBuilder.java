package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.*;
import static java.lang.Thread.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import net.devromik.textFileIndexer.*;
import static net.devromik.textFileIndexer.IndexingStatus.*;
import net.devromik.textFileIndexer.impl.AbstractIndexBuilder;
import static net.devromik.textFileIndexer.impl.utils.IndexEventUtils.*;
import net.devromik.textFileIndexer.utils.*;
import static net.devromik.textFileIndexer.utils.ExceptionUtils.*;
import static net.devromik.textFileIndexer.utils.PreconditionUtils.checkState;
import static net.devromik.textFileIndexer.utils.Slf4jUtils.logException;
import static org.slf4j.LoggerFactory.*;

/**
 * @author Shulnyaev Roman
 */
public final class MainMemorySuffixAutomationIndexBuilder extends AbstractIndexBuilder {

    public MainMemorySuffixAutomationIndexBuilder() {
        this(null, null, null, null);
    }

    public MainMemorySuffixAutomationIndexBuilder(Charset sourceFileEncoding) {
        this(sourceFileEncoding, null);
    }

    public MainMemorySuffixAutomationIndexBuilder(Charset sourceFileEncoding, File targetDirectory) {
        this(null, sourceFileEncoding, targetDirectory, null);
    }

    public MainMemorySuffixAutomationIndexBuilder(
        File sourceFile,
        Charset sourceFileEncoding,
        File targetDirectory,
        IndexEventListener indexEventListener) {

        super(
            sourceFile,
            sourceFileEncoding,
            targetDirectory,
            indexEventListener);
        this.handledSourceFileCharCount = new AtomicInteger();
    }

    /* ***** Реализация IndexBuilder. ***** */

    @Override
    public void build() {
        synchronized (buildingMon) {
            if (getBuildingStatus() != INDEXING_NOT_STARTED) {
                return;
            }

            prepareToBuilding();
            setBuildingStatus(INDEXING_IN_PROGRESS);

            if (!buildingCompleted) {
                notifyOnIndexBuildingStarted(getIndexEventListener(), getSourceFilePath());
                startBuildingThread();
            }
            else {
                automation.startPostbuildOptimizing();
            }
        }
    }

    @Override
    public void cancelBuilding() {
        synchronized (buildingMon) {
            if (getBuildingStatus() != INDEXING_IN_PROGRESS) {
                return;
            }

            setBuildingStatus(INDEXING_CANCELLING_IN_PROGRESS);

            if (buildingCompleted) {
                automation.stopPostbuildOptimizing();
            }
            else {
                stopBuildingThread();
            }

            setBuildingStatus(INDEXING_NOT_STARTED);
            notifyOnIndexBuildingCancelled(getIndexEventListener(), getSourceFilePath());
        }
    }

    @Override
    public Index getIndex() {
        checkPreparedToBuilding();
        return index;
    }

    // ****************************** //

    static final long ERROR_TIMEOUT_IN_MILLIS = 100L;

    /* ***** Задача построения индекса. ***** */

    private static final String BUILDING_THREAD_NAME_PATTERN = "Main Memory Suffix Automation Index Builder [source file pathname = \"{0}\"]";

    private class Building implements Runnable {

        @Override
        public void run() {
            while (!buildingCompleted && !interrupted()) {
                try {
                    int currentCharCode = sourceFileReader.read();

                    if (currentCharCode != END_OF_FILE) {
                        char currentChar = (char)currentCharCode;
                        automation.extend(currentChar);
                        handledSourceFileCharCount.incrementAndGet();
                    }
                    else {
                        synchronized (buildingMon) {
                            buildingCompleted = true;
                            automation.onBuildCompleted();
                        }

                        setBuildingStatus(INDEXING_SUCCESSFULLY_COMPLETED);
                        notifyOnIndexBuildingSuccessfullyCompleted(
                            getIndexEventListener(),
                            getSourceFilePath());
                        return;
                    }
                }
                catch (Exception exception) {
                    logException(logger, exception);
                    notifyOnIndexBuildingErrorOccurred(
                        getIndexEventListener(),
                        getSourceFilePath(),
                        exception);

                    try {
                        sleep(ERROR_TIMEOUT_IN_MILLIS);
                    }
                    catch (InterruptedException interruptedException) {
                        return;
                    }
                }
            }
        }

        // ****************************** //

        private static final int END_OF_FILE = -1;
    }

    int getHandledSourceFileCharCount() {
        return handledSourceFileCharCount.get();
    }

    // ****************************** //

    private void checkSourceFile() {
        try {
            PreconditionUtils.checkSourceFile(getSourceFile());
        }
        catch (Exception exception) {
            throw wrapInIllegalStateException(exception);
        }
    }

    private void prepareToBuilding() {
        if (isPreparedToBuilding()) {
            return;
        }

        checkSourceFile();
        prepareAutomation();
        prepareIndex();
        prepareSourceFileReader();
        setPreparedToBuilding(true);
    }

    private void checkPreparedToBuilding() {
        checkState(preparedToBuilding);
    }

    private void setPreparedToBuilding(boolean preparedToBuilding) {
        this.preparedToBuilding = preparedToBuilding;
    }

    private boolean isPreparedToBuilding() {
        return preparedToBuilding;
    }

    private void prepareSourceFileReader() {
        try {
            Charset sourceFileEncoding = hasSourceFileEncoding() ? getSourceFileEncoding() : defaultCharset();
            sourceFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(getSourceFile()), sourceFileEncoding));
        }
        catch (Exception exception) {
            logException(logger, exception);
            notifyOnIndexBuildingErrorOccurred(getIndexEventListener(), getSourceFilePath(), exception);
            throw wrapInRuntimeException(exception);
        }
    }

    private void prepareAutomation() {
        this.automation = new SuffixAutomation(getSourceFilePath());
    }

    private void prepareIndex() {
        this.index = new MainMemorySuffixAutomationIndex(getSourceFilePath(), automation);
    }

    private String makeBuildingThreadName() {
        return format(BUILDING_THREAD_NAME_PATTERN, getSourceFilePath());
    }

    private void startBuildingThread() {
        if (buildingThread == null) {
            buildingThread = new Thread(new Building(), makeBuildingThreadName());
            buildingThread.start();
        }
    }

    private void stopBuildingThread() {
        buildingThread.interrupt();

        try {
            logger.info("Waiting for building thread \"{}\" stopped...", makeBuildingThreadName());
            buildingThread.join();
            logger.info("Building thread \"{}\" successfully stopped", makeBuildingThreadName());
        }
        catch (InterruptedException exception) {
            logger.error("Interrupted while waiting for building thread \"{}\" stopped", makeBuildingThreadName());
        }
        finally {
            buildingThread = null;
        }
    }

    // ****************************** //

    private SuffixAutomation automation;
    private MainMemorySuffixAutomationIndex index;

    private boolean preparedToBuilding;
    private BufferedReader sourceFileReader;
    private AtomicInteger handledSourceFileCharCount;
    private boolean buildingCompleted;
    private Thread buildingThread;
    private final Object buildingMon = new Object();

    // Логгер.
    private final static Logger logger = getLogger(MainMemorySuffixAutomationIndexBuilder.class);
}
