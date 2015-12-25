package net.devromik.textFileIndexer.utils;

import static java.lang.Integer.*;

/**
 * @author Shulnyaev Roman
 */
public final class OrderedCharIntPair implements Comparable<OrderedCharIntPair> {

    public OrderedCharIntPair(char left, int right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(OrderedCharIntPair rightOrderedCharIntPair) {
        return
            left != rightOrderedCharIntPair.left ?
            compare(left, rightOrderedCharIntPair.left) :
            compare(right, rightOrderedCharIntPair.right);
    }

    // ****************************** //

    public final char left;
    public final int right;
}
