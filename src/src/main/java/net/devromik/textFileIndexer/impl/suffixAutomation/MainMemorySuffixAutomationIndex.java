package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.nio.file.Path;
import net.devromik.textFileIndexer.OccurrencePosIterator;
import net.devromik.textFileIndexer.impl.*;
import net.devromik.textFileIndexer.utils.*;

/**
 * @author Shulnyaev Roman
 */
public final class MainMemorySuffixAutomationIndex extends AbstractIndex {

    public static final int MAX_ACCEPTABLE_SOURCE_FILE_LENGTH = Integer.MAX_VALUE / 2;

    // ****************************** //

    @Override
    public OccurrencePosIterator getOccurrencePosIterator(String soughtForCharSeq) {
        return
            !StringUtils.isNullOrEmpty(soughtForCharSeq) ?
            automation.getOccurrencePosIterator(soughtForCharSeq) :
            EmptyOccurrencePosIterator.INSTANCE;
    }

    // ****************************** //

    MainMemorySuffixAutomationIndex(Path sourceFilePath, SuffixAutomation automation) {
        super(sourceFilePath);
        this.automation = PreconditionUtils.checkNotNull(automation);
    }

    // ****************************** //

    private SuffixAutomation automation;
}
