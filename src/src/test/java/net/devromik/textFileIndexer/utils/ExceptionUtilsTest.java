package net.devromik.textFileIndexer.utils;

import org.junit.Test;
import static net.devromik.textFileIndexer.utils.ExceptionUtils.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ExceptionUtilsTest {

    @Test
    public void test_WrapInRuntimeException_When_WrappingExceptionIsNotNull() throws Exception {
        assertThat(wrapInRuntimeException(new Exception()), instanceOf(RuntimeException.class));
    }

    @Test(expected = NullPointerException.class)
    public void test_WrapInRuntimeException_When_WrappingExceptionIsNull() throws Exception {
        wrapInRuntimeException(null);
    }

    // ****************************** //

    @Test
    public void test_WrapInIllegalStateException_When_WrappingExceptionIsNotNull() throws Exception {
        assertThat(wrapInIllegalStateException(new Exception()), instanceOf(IllegalStateException.class));
    }

    @Test(expected = NullPointerException.class)
    public void test_WrapInIllegalStateException_When_WrappingExceptionIsNull() throws Exception {
        wrapInIllegalStateException(null);
    }
}