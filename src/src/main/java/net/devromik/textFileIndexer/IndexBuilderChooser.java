package net.devromik.textFileIndexer;

import java.nio.file.Path;

/**
 * @author Shulnyaev Roman
 */
public interface IndexBuilderChooser {
    IndexBuilder choose(Path sourceFilePath);
}
