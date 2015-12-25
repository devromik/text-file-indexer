package net.devromik.textFileIndexer.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import net.devromik.textFileIndexer.*;
import net.devromik.textFileIndexer.utils.PreconditionUtils;

/**
 * @author Shulnyaev Roman
 */
public abstract class AbstractIndexBuilder implements IndexBuilder {

    public AbstractIndexBuilder() {
        this(null, null, null, null);
    }

    public AbstractIndexBuilder(
        File sourceFile,
        Charset sourceFileEncoding,
        File targetDirectory,
        IndexEventListener indexEventListener) {

        this.sourceFile = sourceFile;
        this.sourceFileEncoding = sourceFileEncoding;
        this.targetDirectory = targetDirectory;
        this.indexEventListener = indexEventListener;
        this.buildingStatus = IndexingStatus.INDEXING_NOT_STARTED;
    }

    @Override
    public final void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public final File getSourceFile() {
        return sourceFile;
    }

    public final Path getSourceFilePath() {
        return sourceFile != null ? sourceFile.toPath() : null;
    }

    public final boolean hasSourceFileEncoding() {
        return sourceFileEncoding != null;
    }

    public final void setSourceFileEncoding(Charset sourceFileEncoding) {
        this.sourceFileEncoding = sourceFileEncoding;
    }

    public final Charset getSourceFileEncoding() {
        return sourceFileEncoding;
    }

    public final void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public final File getTargetDirectory() {
        return targetDirectory;
    }

    @Override
    public final void setIndexEventListener(IndexEventListener indexEventListener) {
        this.indexEventListener = indexEventListener;
    }

    public final IndexEventListener getIndexEventListener() {
        return indexEventListener;
    }

    protected final void setBuildingStatus(IndexingStatus indexBuildingStatus) {
        this.buildingStatus = PreconditionUtils.checkNotNull(indexBuildingStatus);
    }

    @Override
    public final IndexingStatus getBuildingStatus() {
        return buildingStatus;
    }

    // ****************************** //

    private File sourceFile;
    private Charset sourceFileEncoding;
    private File targetDirectory;
    private IndexEventListener indexEventListener;
    private volatile IndexingStatus buildingStatus;
}
