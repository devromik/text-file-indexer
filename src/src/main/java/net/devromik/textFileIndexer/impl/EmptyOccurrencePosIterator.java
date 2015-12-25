package net.devromik.textFileIndexer.impl;

import net.devromik.textFileIndexer.OccurrencePosIterator;

/**
 * @author Shulnyaev Roman
 */
public enum EmptyOccurrencePosIterator implements OccurrencePosIterator {
    INSTANCE;

    // ****************************** //

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public long getNext() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close() {}
}
