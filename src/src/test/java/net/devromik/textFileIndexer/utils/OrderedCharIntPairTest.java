package net.devromik.textFileIndexer.utils;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class OrderedCharIntPairTest {

    @Test
    public void test_CompareTo() throws Exception {
        assertThat(new OrderedCharIntPair('a', 0).compareTo(new OrderedCharIntPair('a', 0)), is(0));
        assertThat(new OrderedCharIntPair('a', 1).compareTo(new OrderedCharIntPair('a', 0)), is(1));
        assertThat(new OrderedCharIntPair('a', 0).compareTo(new OrderedCharIntPair('a', 1)), is(-1));

        assertThat(new OrderedCharIntPair('a', 0).compareTo(new OrderedCharIntPair('b', 0)), is(-1));
        assertThat(new OrderedCharIntPair('a', 1).compareTo(new OrderedCharIntPair('b', 0)), is(-1));
        assertThat(new OrderedCharIntPair('a', 0).compareTo(new OrderedCharIntPair('b', 1)), is(-1));

        assertThat(new OrderedCharIntPair('b', 0).compareTo(new OrderedCharIntPair('a', 0)), is(1));
        assertThat(new OrderedCharIntPair('b', 1).compareTo(new OrderedCharIntPair('a', 0)), is(1));
        assertThat(new OrderedCharIntPair('b', 0).compareTo(new OrderedCharIntPair('a', 1)), is(1));
    }
}