package net.devromik.textFileIndexer.utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ArrayUtilsTest {

    @Test(expected = NullPointerException.class)
    public void test_GrowIntArrayIfNeeded_When_ArrayIsNull() throws Exception {
        ArrayUtils.growIntArrayIfNeeded(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowIntArrayIfNeeded_When_MinAcceptableArrayLengthIsNegative() throws Exception {
        ArrayUtils.growIntArrayIfNeeded(new int[0], -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowIntArrayIfNeeded_When_MinAcceptableArrayLengthIsZero() throws Exception {
        ArrayUtils.growIntArrayIfNeeded(new int[0], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowIntArrayIfNeeded_When_MinAcceptableArrayLengthIsGreaterThanMaxAcceptableArrayLength() throws Exception {
        ArrayUtils.growIntArrayIfNeeded(new int[0], ArrayUtils.MAX_ACCEPTABLE_ARRAY_LENGTH + 1);
    }

    @Test
    public void test_GrowIntArrayIfNeeded_When_ArrayLengthIsEmpty() throws Exception {
        assertTrue(
            Arrays.equals(
                new int[ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING],
                ArrayUtils.growIntArrayIfNeeded(new int[0], ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING)));
    }

    @Test
    public void test_GrowIntArrayIfNeeded_When_ArrayLengthIsLessThanMinAcceptableArrayLength() throws Exception {
        int[] array = new int[5000];
        int[] grownArray = ArrayUtils.growIntArrayIfNeeded(array, 10000);
        assertTrue(grownArray.length >= 10000);
        assertTrue(Arrays.equals(Arrays.copyOf(grownArray, 5000), array));
    }

    // ****************************** //

    @Test(expected = NullPointerException.class)
    public void test_GrowCharArrayIfNeeded_When_ArrayIsNull() throws Exception {
        ArrayUtils.growCharArrayIfNeeded(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowCharArrayIfNeeded_When_MinAcceptableArrayLengthIsNegative() throws Exception {
        ArrayUtils.growCharArrayIfNeeded(new char[0], -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowCharArrayIfNeeded_When_MinAcceptableArrayLengthIsZero() throws Exception {
        ArrayUtils.growCharArrayIfNeeded(new char[0], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowCharArrayIfNeeded_When_MinAcceptableArrayLengthIsGreaterThanMaxAcceptableArrayLength() throws Exception {
        ArrayUtils.growCharArrayIfNeeded(new char[0], ArrayUtils.MAX_ACCEPTABLE_ARRAY_LENGTH + 1);
    }

    @Test
    public void test_GrowCharArrayIfNeeded_When_ArrayLengthIsEmpty() throws Exception {
        assertTrue(
            Arrays.equals(
                new char[ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING],
                ArrayUtils.growCharArrayIfNeeded(new char[0], ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING)));
    }

    @Test
    public void test_GrowCharArrayIfNeeded_When_ArrayLengthIsLessThanMinAcceptableArrayLength() throws Exception {
        char[] array = new char[5000];
        char[] grownArray = ArrayUtils.growCharArrayIfNeeded(array, 10000);
        assertTrue(grownArray.length >= 10000);
        assertTrue(Arrays.equals(Arrays.copyOf(grownArray, 5000), array));
    }

    // ****************************** //

    @Test(expected = NullPointerException.class)
    public void test_GrowAtomicReferenceArrayIfNeeded_When_ArrayIsNull() throws Exception {
        ArrayUtils.growAtomicReferenceArrayIfNeeded(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowAtomicReferenceArrayIfNeeded_When_MinAcceptableArrayLengthIsNegative() throws Exception {
        ArrayUtils.growAtomicReferenceArrayIfNeeded(new AtomicReferenceArray<>(0), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowAtomicReferenceArrayIfNeeded_When_MinAcceptableArrayLengthIsZero() throws Exception {
        ArrayUtils.growAtomicReferenceArrayIfNeeded(new AtomicReferenceArray<>(0), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_GrowAtomicReferenceArrayIfNeeded_When_MinAcceptableArrayLengthIsGreaterThanMaxAcceptableArrayLength() throws Exception {
        ArrayUtils.growAtomicReferenceArrayIfNeeded(
            new AtomicReferenceArray<>(0),
            ArrayUtils.MAX_ACCEPTABLE_ARRAY_LENGTH + 1);
    }

    @Test
    public void test_GrowAtomicReferenceArrayIfNeeded_When_ArrayLengthIsEmpty() throws Exception {
        AtomicReferenceArray<Object> grownArray = ArrayUtils.growAtomicReferenceArrayIfNeeded(
            new AtomicReferenceArray<>(
                0), ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING);
        assertThat(grownArray.length(), CoreMatchers.is(ArrayUtils.MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING));
    }

    @Test
    public void test_GrowAtomicReferenceArrayIfNeeded_When_ArrayLengthIsLessThanMinAcceptableArrayLength() throws Exception {
        AtomicReferenceArray<Object> array = new AtomicReferenceArray<>(5000);

        for (int i = 0; i < 5000; ++i) {
            array.set(i, i);
        }

        AtomicReferenceArray<Object> grownArray = ArrayUtils.growAtomicReferenceArrayIfNeeded(array, 10000);
        assertTrue(grownArray.length() >= 10000);

        for (int i = 0; i < 10000; ++i) {
            assertTrue((i < 5000 && grownArray.get(i).equals(array.get(i)) || (grownArray.get(i) == null)));
        }
    }
}