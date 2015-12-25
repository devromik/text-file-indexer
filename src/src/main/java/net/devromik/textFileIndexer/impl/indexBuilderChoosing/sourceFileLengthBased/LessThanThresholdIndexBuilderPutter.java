package net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased;

import net.devromik.textFileIndexer.IndexBuilder;
import static net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased.SourceFileLengthBasedIndexBuilderChooser.checkLessThanThreshold;
import static net.devromik.textFileIndexer.utils.PreconditionUtils.*;

/**
 * @author Shulnyaev Roman
 */
public final class LessThanThresholdIndexBuilderPutter {

    public LessThanThresholdIndexBuilderPutter thenChoose(Class<? extends IndexBuilder> indexBuilderClass) {
        checkNotNull(indexBuilderClass);
        checkState(!isPuttingCompleted());
        checkState(!indexBuilderCreationInfo.hasIndexBuilderClass());

        indexBuilderCreationInfo.setIndexBuilderClass(indexBuilderClass);
        return this;
    }

    public void usingDefaultConstructor() {
        usingConstructorParams((Object[])null);
    }

    public void usingConstructorParams(Object... indexBuilderConstructorParams) {
        checkState(!isPuttingCompleted());
        checkState(indexBuilderCreationInfo.hasIndexBuilderClass());

        indexBuilderCreationInfo.setIndexBuilderConstructorParams(indexBuilderConstructorParams);
        chooser.putIndexBuilderCreationInfo(threshold, indexBuilderCreationInfo);
        setPuttingCompleted(true);
    }

    // ****************************** //

    /* package private */ LessThanThresholdIndexBuilderPutter(SourceFileLengthBasedIndexBuilderChooser chooser, long threshold) {
        checkSetChooser(chooser);
        checkSetThreshold(threshold);
        initIndexBuilderCreationInfo();
    }

    // ****************************** //

    private void checkSetChooser(SourceFileLengthBasedIndexBuilderChooser chooser) {
        this.chooser = checkNotNull(chooser);
    }

    private void checkSetThreshold(long threshold) {
        checkLessThanThreshold(threshold);
        this.threshold = threshold;
    }

    private void initIndexBuilderCreationInfo() {
        this.indexBuilderCreationInfo = new IndexBuilderCreationInfo();
    }

    private void setPuttingCompleted(boolean puttingCompleted) {
        this.puttingCompleted = puttingCompleted;
    }

    private boolean isPuttingCompleted() {
        return puttingCompleted;
    }

    // ****************************** //

    private SourceFileLengthBasedIndexBuilderChooser chooser;
    private long threshold;
    private IndexBuilderCreationInfo indexBuilderCreationInfo;
    private boolean puttingCompleted;
}
