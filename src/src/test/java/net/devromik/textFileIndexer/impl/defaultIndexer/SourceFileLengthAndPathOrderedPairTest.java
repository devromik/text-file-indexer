package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.nio.file.Paths;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SourceFileLengthAndPathOrderedPairTest {

    @Test
    public void testCompareTo() throws Exception {
        assertThat(makeInstance(0L, "/a").compareTo(makeInstance(0L, "/a")), is(0));
        assertThat(makeInstance(0L, "/b").compareTo(makeInstance(0L, "/a")), is(1));
        assertThat(makeInstance(0L, "/a").compareTo(makeInstance(0L, "/b")), is(-1));

        assertThat(makeInstance(0L, "/a").compareTo(makeInstance(1L, "/a")), is(-1));
        assertThat(makeInstance(0L, "/b").compareTo(makeInstance(1L, "/a")), is(-1));
        assertThat(makeInstance(0L, "/a").compareTo(makeInstance(1L, "/b")), is(-1));

        assertThat(makeInstance(1L, "/a").compareTo(makeInstance(0L, "/a")), is(1));
        assertThat(makeInstance(1L, "/b").compareTo(makeInstance(0L, "/a")), is(1));
        assertThat(makeInstance(1L, "/a").compareTo(makeInstance(0L, "/b")), is(1));
    }

    private SourceFileLengthAndPathOrderedPair makeInstance(long sourceFileLength, String sourceFilePath) {
        return new SourceFileLengthAndPathOrderedPair(sourceFileLength, Paths.get(sourceFilePath));
    }
}