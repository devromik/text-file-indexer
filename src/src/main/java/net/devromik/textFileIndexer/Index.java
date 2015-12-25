package net.devromik.textFileIndexer;

import java.nio.file.Path;

/**
 * @author Shulnyaev Roman
 */
public interface Index {
    Path getSourceFilePath();
    OccurrencePosIterator getOccurrencePosIterator(String soughtForCharSeq);
}
