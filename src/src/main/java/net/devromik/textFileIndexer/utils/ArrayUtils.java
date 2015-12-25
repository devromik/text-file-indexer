package net.devromik.textFileIndexer.utils;

import java.util.concurrent.atomic.AtomicReferenceArray;
import static java.lang.Math.*;
import static java.util.Arrays.copyOf;

/**
 * @author Shulnyaev Roman
 */
public final class ArrayUtils {

    public static final int MAX_ACCEPTABLE_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    public static final int MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING = 32;

    // ****************************** //

    /**
     * Если размер array меньше, чем minAcceptableArrayLength, создает и возвращает новый массив grownArray такой, что:
     *     - grownArray.length >= minAcceptableArrayLength.
     *     - подмассив grownArray[0, array.length - 1] поэлементно равен array.
     *     - подмассив grownArray[array.length, grownArray.length - 1] содержит исключительно нули.
     * Если размер array не меньше, чем minAcceptableArrayLength, возвращает array.
     *
     * @throws java.lang.NullPointerException если array == null.
     * @throws java.lang.IllegalArgumentException если minAcceptableArrayLength <= 0 || minAcceptableArrayLength > MAX_ACCEPTABLE_ARRAY_SIZE.
     */
    public static int[] growIntArrayIfNeeded(int[] array, int minAcceptableArrayLength) {
        PreconditionUtils.checkNotNull(array);
        PreconditionUtils.checkArgument(minAcceptableArrayLength > 0 && minAcceptableArrayLength <= MAX_ACCEPTABLE_ARRAY_LENGTH);

        int arrayLength = array.length;

        if (arrayLength >= minAcceptableArrayLength) {
            return array;
        }

        int grownArrayLength =
            arrayLength +
            min(
                MAX_ACCEPTABLE_ARRAY_LENGTH - arrayLength,
                max(
                    max(arrayLength >> 1, minAcceptableArrayLength - arrayLength),
                    MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING));

        return copyOf(array, grownArrayLength);
    }

    /**
     * Если размер array меньше, чем minAcceptableArrayLength, создает и возвращает новый массив grownArray такой, что:
     *     - grownArray.length >= minAcceptableArrayLength.
     *     - подмассив grownArray[0, array.length - 1] поэлементно равен array.
     *     - подмассив grownArray[array.length, grownArray.length - 1] содержит исключительно '\\u000'.
     * Если размер array не меньше, чем minAcceptableArrayLength, возвращает array.
     *
     * @throws java.lang.NullPointerException если array == null.
     * @throws java.lang.IllegalArgumentException если minAcceptableArrayLength <= 0 || minAcceptableArrayLength > MAX_ACCEPTABLE_ARRAY_SIZE.
     */
    public static char[] growCharArrayIfNeeded(char[] array, int minAcceptableArrayLength) {
        PreconditionUtils.checkNotNull(array);
        PreconditionUtils.checkArgument(minAcceptableArrayLength > 0 && minAcceptableArrayLength <= MAX_ACCEPTABLE_ARRAY_LENGTH);

        int arrayLength = array.length;

        if (arrayLength >= minAcceptableArrayLength) {
            return array;
        }

        int grownArrayLength =
            arrayLength +
            min(
                MAX_ACCEPTABLE_ARRAY_LENGTH - arrayLength,
                max(
                    max(arrayLength >> 1, minAcceptableArrayLength - arrayLength),
                    MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING));

        return copyOf(array, grownArrayLength);
    }

    /**
     * Если размер array меньше, чем minAcceptableArrayLength, создает и возвращает новый массив grownArray такой, что:
     *     - grownArray.length >= minAcceptableArrayLength.
     *     - подмассив grownArray[0, array.length - 1] поэлементно равен array.
     *     - подмассив grownArray[array.length, grownArray.length - 1] содержит исключительно null.
     * Если размер array не меньше, чем minAcceptableArrayLength, возвращает array.
     *
     * @throws java.lang.NullPointerException если array == null.
     * @throws java.lang.IllegalArgumentException если minAcceptableArrayLength <= 0 || minAcceptableArrayLength > MAX_ACCEPTABLE_ARRAY_SIZE.
     */
    public static <T> AtomicReferenceArray<T> growAtomicReferenceArrayIfNeeded(AtomicReferenceArray<T> array, int minAcceptableArrayLength) {
        PreconditionUtils.checkNotNull(array);
        PreconditionUtils.checkArgument(minAcceptableArrayLength > 0 && minAcceptableArrayLength <= MAX_ACCEPTABLE_ARRAY_LENGTH);
        
        int arrayLength = array.length();

        if (arrayLength >= minAcceptableArrayLength) {
            return array;
        }

        int grownArrayLength =
            arrayLength +
            min(
                MAX_ACCEPTABLE_ARRAY_LENGTH - arrayLength,
                max(
                    max(arrayLength >> 1, minAcceptableArrayLength - arrayLength),
                    MIN_ARRAY_LENGTH_INCREASING_WHILE_GROWING));
        AtomicReferenceArray<T> grownArray = new AtomicReferenceArray<>(grownArrayLength);

        for (int i = 0; i < arrayLength; ++i) {
            grownArray.set(i, array.get(i));
        }

        return grownArray;
    }

    // ****************************** //

    private ArrayUtils() {}
}
