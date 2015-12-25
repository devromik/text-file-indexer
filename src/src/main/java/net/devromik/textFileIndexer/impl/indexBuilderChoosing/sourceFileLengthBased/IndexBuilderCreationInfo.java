package net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased;

import net.devromik.textFileIndexer.IndexBuilder;

/**
 * @author Shulnyaev Roman
 */
final class IndexBuilderCreationInfo {

    boolean hasIndexBuilderClass() {
        return indexBuilderClass != null;
    }

    void setIndexBuilderClass(Class<? extends IndexBuilder> indexBuilderClass) {
        this.indexBuilderClass = indexBuilderClass;
    }

    Class<? extends IndexBuilder> getIndexBuilderClass() {
        return indexBuilderClass;
    }

    void setIndexBuilderConstructorParams(Object[] indexBuilderConstructorParams) {
        this.indexBuilderConstructorParams = indexBuilderConstructorParams;
    }

    Object[] getIndexBuilderConstructorParams() {
        return indexBuilderConstructorParams;
    }

    // ****************************** //

    private Class<? extends IndexBuilder> indexBuilderClass;
    private Object[] indexBuilderConstructorParams;
}
