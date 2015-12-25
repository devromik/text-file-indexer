package net.devromik.textFileIndexer.impl.defaultIndexer;

/**
 * @author Shulnyaev Roman
 */
final class SourceFileInfo {

    SourceFileInfo(long sourceFileLength) {
        this.sourceFileLength = sourceFileLength;
    }

    long getSourceFileLength() {
        return sourceFileLength;
    }

    // ****************************** //

    final long sourceFileLength;
}
