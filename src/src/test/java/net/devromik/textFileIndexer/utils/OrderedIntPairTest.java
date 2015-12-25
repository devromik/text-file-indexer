package net.devromik.textFileIndexer.utils;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class OrderedIntPairTest {

    @Test
    public void test_CompareTo() throws Exception {
        assertThat(new OrderedIntPair(0, 0).compareTo(new OrderedIntPair(0, 0)), is(0));
        assertThat(new OrderedIntPair(0, 1).compareTo(new OrderedIntPair(0, 0)), is(1));
        assertThat(new OrderedIntPair(0, 0).compareTo(new OrderedIntPair(0, 1)), is(-1));

        assertThat(new OrderedIntPair(0, 0).compareTo(new OrderedIntPair(1, 0)), is(-1));
        assertThat(new OrderedIntPair(0, 1).compareTo(new OrderedIntPair(1, 0)), is(-1));
        assertThat(new OrderedIntPair(0, 0).compareTo(new OrderedIntPair(1, 1)), is(-1));

        assertThat(new OrderedIntPair(1, 0).compareTo(new OrderedIntPair(0, 0)), is(1));
        assertThat(new OrderedIntPair(1, 1).compareTo(new OrderedIntPair(0, 0)), is(1));
        assertThat(new OrderedIntPair(1, 0).compareTo(new OrderedIntPair(0, 1)), is(1));
    }
}