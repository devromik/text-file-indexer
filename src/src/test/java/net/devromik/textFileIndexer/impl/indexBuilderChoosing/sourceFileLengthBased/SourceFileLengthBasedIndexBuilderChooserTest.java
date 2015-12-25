package net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased;

import java.io.File;
import java.nio.file.Path;
import org.junit.*;
import net.devromik.textFileIndexer.Index;
import net.devromik.textFileIndexer.impl.AbstractIndexBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SourceFileLengthBasedIndexBuilderChooserTest {

    @Test
    public void test_CheckLessThanThreshold_When_ThresholdIsEqualToMinAcceptable() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser.checkLessThanThreshold(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD);
    }

    @Test
    public void test_CheckLessThanThreshold_When_ThresholdIsGreaterThanMinAcceptable() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser.checkLessThanThreshold(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckLessThanThreshold_When_ThresholdIsLessThanMinAcceptable() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser.checkLessThanThreshold(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD - 1);
    }

    // ****************************** //

    @Test(expected = IllegalStateException.class)
    public void test_Choose_When_NoRegisteredIndexBuilders() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser chooser = new SourceFileLengthBasedIndexBuilderChooser();
        chooser.choose(makeSourceFilePathMock(0L));
    }

    @Test
    public void test_Choose_When_OneIndexBuilderRegistered() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser chooser = new SourceFileLengthBasedIndexBuilderChooser();
        chooser.whenSourceFileLengthLessThan(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD).thenChoose(IndexBuilderMock_A.class).usingDefaultConstructor();
        assertThat(chooser.choose(makeSourceFilePathMock(1000L)), instanceOf(IndexBuilderMock_A.class));
    }

    @Test
    public void test_Choose_When_ManyIndexBuilderRegistered() throws Exception {
        SourceFileLengthBasedIndexBuilderChooser chooser = new SourceFileLengthBasedIndexBuilderChooser();
        chooser.whenSourceFileLengthLessThan(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD).thenChoose(IndexBuilderMock_A.class).usingDefaultConstructor();
        chooser.whenSourceFileLengthLessThan(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD + 1).thenChoose(IndexBuilderMock_B.class).usingConstructorParams(1);
        chooser.whenSourceFileLengthLessThan(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD + 2).thenChoose(IndexBuilderMock_C.class).usingConstructorParams(1, 2);

        assertThat(chooser.choose(makeSourceFilePathMock(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD - 1)), instanceOf(IndexBuilderMock_A.class));
        assertThat(chooser.choose(makeSourceFilePathMock(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD)), instanceOf(IndexBuilderMock_B.class));
        assertThat(chooser.choose(makeSourceFilePathMock(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD + 1)), instanceOf(IndexBuilderMock_C.class));
        assertThat(chooser.choose(makeSourceFilePathMock(SourceFileLengthBasedIndexBuilderChooser.MIN_SOURCE_FILE_LENGTH_LESS_THAN_THRESHOLD + 1000)), instanceOf(IndexBuilderMock_C.class));
    }

    private Path makeSourceFilePathMock(long sourceFileLength) {
        File sourceFileMock = mock(File.class);
        when(sourceFileMock.isDirectory()).thenReturn(false);
        when(sourceFileMock.canRead()).thenReturn(true);
        when(sourceFileMock.length()).thenReturn(sourceFileLength);
        Path sourceFilePathMock = mock(Path.class);
        when(sourceFilePathMock.toFile()).thenReturn(sourceFileMock);

        return sourceFilePathMock;
    }

    // ****************************** //

    public static class IndexBuilderMock_A extends AbstractIndexBuilder {
        public IndexBuilderMock_A() {}

        @Override public void build() {}
        @Override public void cancelBuilding() {}
        @Override public Index getIndex() { return null; }
    }

    public static class IndexBuilderMock_B extends IndexBuilderMock_A {
        public IndexBuilderMock_B(int fakeArg) {}
    }

    public static class IndexBuilderMock_C extends IndexBuilderMock_A {
        public IndexBuilderMock_C(int fakeArg1, int fakeArg2) {}
    }
}