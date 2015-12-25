package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.nio.file.Path;
import java.util.Collection;
import net.devromik.textFileIndexer.IndexBuilderChooser;
import net.devromik.textFileIndexer.utils.PreconditionUtils;

/**
 * @author Shulnyaev Roman
 */
public final class DefaultIndexerBuilder {

    public DefaultIndexerBuilder(
        Collection<? extends Path> sourceFilePaths,
        IndexBuilderChooser indexBuilderChooser) {

        PreconditionUtils.checkArgument(
            sourceFilePaths != null &&
                !sourceFilePaths.isEmpty() &&
                sourceFilePaths.size() <= DefaultIndexer.MAX_SOURCE_FILE_COUNT);
        this.sourceFilePaths = sourceFilePaths;

        this.indexBuilderChooser = PreconditionUtils.checkNotNull(indexBuilderChooser);
    }

    public DefaultIndexerBuilder usingMaxSimultaneousIndexBuildingCount(int maxSimultaneousIndexBuildingCount) {
        PreconditionUtils.checkArgument(
            maxSimultaneousIndexBuildingCount >= DefaultIndexer.MIN_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT &&
                maxSimultaneousIndexBuildingCount <= DefaultIndexer.MAX_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT);
        this.maxSimultaneousIndexBuildingCount = maxSimultaneousIndexBuildingCount;
        return this;
    }

    public DefaultIndexer buildIndexer() {
        return new
            DefaultIndexer(
                sourceFilePaths,
                indexBuilderChooser,
                maxSimultaneousIndexBuildingCount);
    }

    // ****************************** //

    private final Collection<? extends Path> sourceFilePaths;
    private final IndexBuilderChooser indexBuilderChooser;
    private int maxSimultaneousIndexBuildingCount = DefaultIndexer.DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT;
}
