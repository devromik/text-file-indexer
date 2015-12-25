package net.devromik.textFileIndexer.impl.karkkainenSanders;

import java.io.File;
import java.nio.charset.Charset;
import net.devromik.textFileIndexer.*;
import net.devromik.textFileIndexer.impl.AbstractIndexBuilder;

/**
 * @author Shulnyaev Roman
 */
public final class ExternalMemoryKarkkainenSandersIndexBuilder extends AbstractIndexBuilder {

    public ExternalMemoryKarkkainenSandersIndexBuilder() {
        this(null, null, null, null);
    }

    public ExternalMemoryKarkkainenSandersIndexBuilder(
        File sourceFile,
        Charset sourceFileEncoding,
        File targetDirectory,
        IndexEventListener indexEventListener) {

        super(
            sourceFile,
            sourceFileEncoding,
            targetDirectory,
            indexEventListener);
    }

    @Override
    public void build() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void cancelBuilding() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Index getIndex() {
        throw new UnsupportedOperationException("Not supported");
    }
}
