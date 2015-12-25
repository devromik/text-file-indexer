package net.devromik.textFileIndexer.impl.defaultIndexer;

import java.nio.file.*;
import java.util.*;
import org.junit.*;
import static java.util.Collections.*;
import net.devromik.textFileIndexer.impl.indexBuilderChoosing.sourceFileLengthBased.SourceFileLengthBasedIndexBuilderChooser;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class DefaultIndexerBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void test_Constructor_With_Params_SourceFilePaths_And_IndexBuilderChooser_When_SourceFilePathsIsNull() throws Exception {
        new DefaultIndexerBuilder(null, new SourceFileLengthBasedIndexBuilderChooser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_Constructor_With_Params_SourceFilePaths_And_IndexBuilderChooser_When_SourceFilePathsIsEmpty() throws Exception {
        new DefaultIndexerBuilder(new ArrayList<Path>(), new SourceFileLengthBasedIndexBuilderChooser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_Constructor_With_Params_SourceFilePaths_And_IndexBuilderChooser_When_SourceFilePathCount_IsGreaterThanMaxAcceptable() throws Exception {
        List<Path> sourceFilePaths = nCopies(DefaultIndexer.MAX_SOURCE_FILE_COUNT + 1, Paths.get("/a"));
        new DefaultIndexerBuilder(sourceFilePaths, new SourceFileLengthBasedIndexBuilderChooser());
    }

    @Test(expected = NullPointerException.class)
    public void test_Constructor_With_Params_SourceFilePaths_And_IndexBuilderChooser_When_IndexBuilderChooserIsNull() throws Exception {
        List<Path> sourceFilePaths = nCopies(1, Paths.get("/a"));
        new DefaultIndexerBuilder(sourceFilePaths, null);
    }

    // ****************************** //

    @Test(expected = IllegalArgumentException.class)
    public void test_UsingMaxSimultaneousIndexBuildingCount_When_MaxSimultaneousIndexBuildingCount_IsLessThanMinAcceptable() throws Exception {
        makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.MIN_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT - 1);
    }

    @Test
    public void test_UsingMaxSimultaneousIndexBuildingCount_When_MaxSimultaneousIndexBuildingCount_IsEqualToMinAcceptable() throws Exception {
        makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.MIN_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT);
    }

    @Test
    public void test_UsingMaxSimultaneousIndexBuildingCount_When_MaxSimultaneousIndexBuildingCount_IsGreaterThanMinAcceptable_And_LessThanMaxAcceptable() throws Exception {
        makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.MIN_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT + 1);
    }

    @Test
    public void test_UsingMaxSimultaneousIndexBuildingCount_When_MaxSimultaneousIndexBuildingCount_IsEqualToMaxAcceptable() throws Exception {
        makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.MAX_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_UsingMaxSimultaneousIndexBuildingCount_When_MaxSimultaneousIndexBuildingCount_IsGreaterThanMaxAcceptable() throws Exception {
        makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.MAX_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT + 1);
    }

    // ****************************** //

    @Test
    public void test_IndexerBuilding() throws Exception {
        assertThat(
            makeInstance().usingMaxSimultaneousIndexBuildingCount(DefaultIndexer.DEFAULT_MAX_SIMULTANEOUS_INDEX_BUILDING_COUNT).buildIndexer(),
            instanceOf(DefaultIndexer.class));
    }

    // ****************************** //

    private DefaultIndexerBuilder makeInstance() {
        List<Path> sourceFilePaths = nCopies(1, Paths.get("/a"));
        return new DefaultIndexerBuilder(sourceFilePaths, new SourceFileLengthBasedIndexBuilderChooser());
    }
}