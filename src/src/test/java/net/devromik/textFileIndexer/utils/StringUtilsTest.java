package net.devromik.textFileIndexer.utils;

import org.junit.Test;
import static net.devromik.textFileIndexer.utils.StringUtils.isNullOrEmpty;
import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void test_IsNullOrEmpty_When_StringUnderTestIsNull() throws Exception {
        assertTrue(isNullOrEmpty(null));
    }

    @Test
    public void test_IsNullOrEmpty_When_StringUnderTestIsEmpty() throws Exception {
        assertTrue(isNullOrEmpty(""));
    }

    @Test
    public void test_IsNullOrEmpty_When_StringUnderTestIsNotNullOrEmpty() throws Exception {
        assertFalse(isNullOrEmpty("ABC"));
    }
}