package net.devromik.textFileIndexer.utils;

import static java.lang.Integer.compare;

/**
 * @author Shulnyaev Roman
 */
public final class OrderedIntPair implements Comparable<OrderedIntPair> {

    public OrderedIntPair(int left, int right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(OrderedIntPair rightOrderedIntPair) {
        return
            left != rightOrderedIntPair.left ?
            compare(left, rightOrderedIntPair.left) :
            compare(right, rightOrderedIntPair.right);
    }

    // ****************************** //

    public final int left;
    public final int right;
}
