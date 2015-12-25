package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.nio.file.Path;
import static java.lang.Long.compare;
import static net.devromik.textFileIndexer.utils.PreconditionUtils.*;

/**
 * @author Shulnyaev Roman (Roman.Shulnyaev@billing.ru)
 */
final class SourceFileLengthAndPathOrderedPair implements Comparable<SourceFileLengthAndPathOrderedPair> {

    SourceFileLengthAndPathOrderedPair(long sourceFileLength, Path sourceFilePath) {
        this.sourceFileLength = checkNonNegative(sourceFileLength);
        this.sourceFilePath = checkNotNull(sourceFilePath);
    }

    @Override
    public int compareTo(SourceFileLengthAndPathOrderedPair right) {
        return
            sourceFileLength != right.sourceFileLength ?
            compare(sourceFileLength, right.sourceFileLength) :
            sourceFilePath.compareTo(right.sourceFilePath);
    }

    // ****************************** //

    final long sourceFileLength;
    final Path sourceFilePath;
}
