package net.devromik.textFileIndexer.impl;

import java.nio.file.Path;
import net.devromik.textFileIndexer.Index;
import net.devromik.textFileIndexer.utils.PreconditionUtils;

/**
 * @author Shulnyaev Roman
 */
public abstract class AbstractIndex implements Index {

    protected AbstractIndex(Path sourceFilePath) {
        this.sourceFilePath = PreconditionUtils.checkNotNull(sourceFilePath);
    }

    @Override
    public Path getSourceFilePath() {
        return sourceFilePath;
    }

    // ****************************** //

    private final Path sourceFilePath;
}
