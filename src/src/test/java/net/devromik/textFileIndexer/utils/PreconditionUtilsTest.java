package net.devromik.textFileIndexer.utils;

import java.io.*;
import org.junit.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PreconditionUtilsTest {

    @Test
    public void test_CheckNotNull_When_ReferenceUnderTestIsNotNull() {
        Object referenceUnderTest = new Object();
        assertThat(PreconditionUtils.checkNotNull(referenceUnderTest), is(referenceUnderTest));
    }

    @Test(expected = NullPointerException.class)
    public void test_CheckNotNull_When_ReferenceUnderTestIsNull() {
        PreconditionUtils.checkNotNull(null);
    }

    // ****************************** //

    @Test
    public void test_CheckArgument_When_ArgumentPreconditionIsSatisfied() {
        PreconditionUtils.checkArgument(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckArgument_When_ArgumentPreconditionIsNotSatisfied() {
        PreconditionUtils.checkArgument(false);
    }

    // ****************************** //

    @Test
    public void test_CheckState_When_StatePreconditionIsSatisfied() {
        PreconditionUtils.checkState(true);
    }

    @Test(expected = IllegalStateException.class)
    public void test_CheckState_When_StatePreconditionIsNotSatisfied() {
        PreconditionUtils.checkState(false);
    }

    // ****************************** //

    @Test
    public void test_CheckNonNegative_When_LongUnderTestIsPositive() {
        assertThat(PreconditionUtils.checkNonNegative(1L), is(1L));
        assertThat(PreconditionUtils.checkNonNegative(Long.MAX_VALUE), is(Long.MAX_VALUE));
    }

    @Test
    public void test_CheckNonNegative_When_LongUnderTestIsZero() {
        assertThat(PreconditionUtils.checkNonNegative(0L), is(0L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckNonNegative_When_LongUnderTestIsNegative() {
        PreconditionUtils.checkNonNegative(-1L);
    }

    // ****************************** //

    @Test
    public void test_CheckSourceFile_When_SourceFileIsCorrect() throws Exception {
        File sourceFile = makeFileMock(false, true, true);
        PreconditionUtils.checkSourceFile(sourceFile);
    }

    @Test(expected = NullPointerException.class)
    public void test_CheckSourceFile_When_SourceFileIsNull() throws Exception {
        PreconditionUtils.checkSourceFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckSourceFile_When_SourceFileIsDirectory() throws Exception {
        File sourceFile = makeFileMock(true, true, true);
        PreconditionUtils.checkSourceFile(sourceFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckSourceFile_When_SourceFileIsNotReadable() throws Exception {
        File sourceFile = makeFileMock(false, false, true);
        PreconditionUtils.checkSourceFile(sourceFile);
    }

    // ****************************** //

    @Test
    public void test_CheckTargetDirectory_When_TargetDirectoryIsCorrect() throws Exception {
        File targetDirectory = makeFileMock(true, true, true);
        PreconditionUtils.checkTargetDirectory(targetDirectory);
    }

    @Test(expected = NullPointerException.class)
    public void test_CheckTargetDirectory_When_TargetDirectoryIsNull() throws Exception {
        PreconditionUtils.checkTargetDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckTargetDirectory_When_TargetDirectoryIsNotDirectory() throws Exception {
        File targetDirectory = makeFileMock(false, true, true);
        PreconditionUtils.checkTargetDirectory(targetDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckTargetDirectory_When_TargetDirectoryIsNotReadable() throws Exception {
        File targetDirectory = makeFileMock(true, false, true);
        PreconditionUtils.checkTargetDirectory(targetDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CheckTargetDirectory_When_TargetDirectoryIsNotWritable() throws Exception {
        File targetDirectory = makeFileMock(true, true, false);
        PreconditionUtils.checkTargetDirectory(targetDirectory);
    }

    // ****************************** //

    public static File makeFileMock(boolean isDirectory, boolean isReadable, boolean isWritable) {
        File fileMock = mock(File.class);
        when(fileMock.isFile()).thenReturn(!isDirectory);
        when(fileMock.isDirectory()).thenReturn(isDirectory);
        when(fileMock.canRead()).thenReturn(isReadable);
        when(fileMock.canWrite()).thenReturn(isWritable);

        return fileMock;
    }
}