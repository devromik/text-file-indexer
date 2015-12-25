package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.util.*;
import static java.util.Arrays.binarySearch;
import net.devromik.textFileIndexer.utils.OrderedCharIntPair;

/**
 * Состояние суффиксного автомата в окончательной форме.
 * Содержит ту и только ту информацию, которая необходима для эффективного поиска всех вхождений заданной строки.
 */
final class DefinitiveState implements State {

    DefinitiveState(
        State buildingTimeState,
        int[] globalInvertedSuffixLinkArray,
        int globalInvertedSuffixLinkArrayFirstFreePos,
        char[] globalTransitionCharArray,
        int[] globalTransitionTargetStateIdArray,
        int globalTransitionArrayFirstFreePos,
        int lastStateId) {

        this.minSubstringEndPos = buildingTimeState.getMinSubstringEndPos();
        moveInvertedSuffixLinksToGlobalArray(
            buildingTimeState,
            globalInvertedSuffixLinkArray,
            globalInvertedSuffixLinkArrayFirstFreePos,
            lastStateId);
        moveTransitionsToGlobalArray(
            buildingTimeState,
            globalTransitionCharArray,
            globalTransitionTargetStateIdArray,
            globalTransitionArrayFirstFreePos,
            lastStateId);
        this.clone = buildingTimeState.isClone();
    }

    private void moveInvertedSuffixLinksToGlobalArray(
        State buildingTimeState,
        int[] globalInvertedSuffixLinkArray,
        int globalInvertedSuffixLinkArrayFirstFreePos,
        int lastStateId) {

        this.globalInvertedSuffixLinkArray = globalInvertedSuffixLinkArray;
        this.globalInvertedSuffixLinkArrayFirstPos = globalInvertedSuffixLinkArrayFirstFreePos;
        int actualInvertedSuffixLinkCount = 0;
        Iterator<Integer> actualInvertedSuffixLinksIter = buildingTimeState.getActualInvertedSuffixLinksIterator(lastStateId);

        while (actualInvertedSuffixLinksIter.hasNext()) {
            this.globalInvertedSuffixLinkArray[this.globalInvertedSuffixLinkArrayFirstPos + actualInvertedSuffixLinkCount++] = actualInvertedSuffixLinksIter.next();
        }

        this.globalInvertedSuffixLinkArrayFollowingLastPos = this.globalInvertedSuffixLinkArrayFirstPos + actualInvertedSuffixLinkCount;
    }

    private void moveTransitionsToGlobalArray(
        State buildingTimeState,
        char[] globalTransitionCharArray,
        int[] globalTransitionTargetStateIdArray,
        int globalTransitionArrayFirstFreePos,
        int lastStateId) {

        this.globalTransitionCharArray = globalTransitionCharArray;
        this.globalTransitionTargetStateIdArray = globalTransitionTargetStateIdArray;
        this.globalTransitionArrayFirstPos = globalTransitionArrayFirstFreePos;
        int actualTransitionCount = 0;
        Iterator<OrderedCharIntPair> actualTransitionIter = buildingTimeState.getActualTransitionsIterator(lastStateId);

        while (actualTransitionIter.hasNext()) {
            OrderedCharIntPair actualTransition = actualTransitionIter.next();
            this.globalTransitionCharArray[this.globalTransitionArrayFirstPos + actualTransitionCount] = actualTransition.left;
            this.globalTransitionTargetStateIdArray[this.globalTransitionArrayFirstPos + actualTransitionCount] = actualTransition.right;
            ++actualTransitionCount;
        }

        this.globalTransitionArrayFollowingLastPos = this.globalTransitionArrayFirstPos + actualTransitionCount;
    }

    @Override
    public int getMinSubstringEndPos() {
        return minSubstringEndPos;
    }

    @Override
    public Iterator<Integer> getActualInvertedSuffixLinksIterator(int automationBuildStep) {
        return new InvertedSuffixLinksIterator();
    }

    @Override
    public boolean hasActualTransition(char transitionChar, int automationBuildStep) {
        return getActualTransitionTargetStateId(transitionChar, automationBuildStep) != NO_STATE_ID;
    }

    @Override
    public int getActualTransitionTargetStateId(char transitionChar, int automationBuildStep) {
        if (!hasTransitions()) {
            return NO_STATE_ID;
        }

        int transitionCharPos =
            binarySearch(
                globalTransitionCharArray,
                globalTransitionArrayFirstPos,
                globalTransitionArrayFollowingLastPos,
                transitionChar);

        if (transitionCharPos >= globalTransitionArrayFirstPos &&
            transitionCharPos < globalTransitionArrayFollowingLastPos &&
            globalTransitionCharArray[transitionCharPos] == transitionChar) {

            return globalTransitionTargetStateIdArray[transitionCharPos];
        }
        else {
            return NO_STATE_ID;
        }
    }

    private boolean hasTransitions() {
        return globalTransitionArrayFirstPos != globalTransitionArrayFollowingLastPos;
    }

    @Override
    public boolean isClone() {
         return clone;
    }

    // ****************************** //

    int getGlobalInvertedSuffixLinkArrayFollowingLastPos() {
        return globalInvertedSuffixLinkArrayFollowingLastPos;
    }

    int getGlobalTransitionArrayFollowingLastPos() {
        return globalTransitionArrayFollowingLastPos;
    }

    void setGlobalInvertedSuffixLinkArray(int[] globalInvertedSuffixLinkArray) {
        this.globalInvertedSuffixLinkArray = globalInvertedSuffixLinkArray;
    }

    void setGlobalTransitionCharArray(char[] globalTransitionCharArray) {
        this.globalTransitionCharArray = globalTransitionCharArray;
    }

    void setGlobalTransitionTargetStateIdArray(int[] globalTransitionTargetStateIdArray) {
        this.globalTransitionTargetStateIdArray = globalTransitionTargetStateIdArray;
    }

    /**
     * Итератор по обратным суффиксным ссылкам.
     */
    private final class InvertedSuffixLinksIterator implements Iterator<Integer> {

        InvertedSuffixLinksIterator() {
            nextInvertedSuffixLinkPos = globalInvertedSuffixLinkArrayFirstPos;
        }

        @Override
        public boolean hasNext() {
            return nextInvertedSuffixLinkPos < globalInvertedSuffixLinkArrayFollowingLastPos;
        }

        @Override
        public Integer next() {
            return hasNext() ? globalInvertedSuffixLinkArray[nextInvertedSuffixLinkPos++] : null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

        // ****************************** //

        private int nextInvertedSuffixLinkPos;
    }

    /* ***** Неподдерживаемые операции. ***** */

    @Override public int getGreatestSubstringLength() { throw new UnsupportedOperationException("Not supported"); }
    @Override public void setSuffixLink(int targetStateId) { throw new UnsupportedOperationException("Not supported"); }
    @Override public int getSuffixLink() { throw new UnsupportedOperationException("Not supported"); }
    @Override public void addInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep) { throw new UnsupportedOperationException("Not supported"); }
    @Override public void removeInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep) { throw new UnsupportedOperationException("Not supported"); }
    @Override public int getActualInvertedSuffixLinkCount(int automationBuildStep) { throw new UnsupportedOperationException("Not supported");}
    @Override public void addTransition(char transitionChar, int targetStateId, int currentAutomationBuildStep) { throw new UnsupportedOperationException("Not supported"); }
    @Override public void redirectTransition(char transitionChar, int newTargetStateId, int currentAutomationBuildStep) { throw new UnsupportedOperationException("Not supported"); }
    @Override public Iterator<OrderedCharIntPair> getActualTransitionsIterator(int automationBuildStep) { throw new UnsupportedOperationException("Not supported"); }
    @Override public int getActualTransitionCount(int automationBuildStep) { throw new UnsupportedOperationException("Not supported"); }

    // ****************************** //

    private int minSubstringEndPos;

    private volatile int[] globalInvertedSuffixLinkArray;
    private int globalInvertedSuffixLinkArrayFirstPos;
    private int globalInvertedSuffixLinkArrayFollowingLastPos;

    private volatile char[] globalTransitionCharArray;
    private volatile int[] globalTransitionTargetStateIdArray;
    private int globalTransitionArrayFirstPos;
    private int globalTransitionArrayFollowingLastPos;

    private boolean clone;
}
