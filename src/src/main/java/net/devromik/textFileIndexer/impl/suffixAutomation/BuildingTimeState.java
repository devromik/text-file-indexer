package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.util.*;
import java.util.concurrent.*;
import static net.devromik.textFileIndexer.impl.suffixAutomation.SuffixAutomation.*;
import net.devromik.textFileIndexer.utils.*;

/**
 * Состояние суффиксного автомата времени построения суффиксного автомата.
 *
 * Менее эффективное по скорости и потреблению памяти, чем DefinitiveState,
 * но допускающее выполняющиеся "одновременно" построение суффиксного автомата и чтение из него (поиск всех вхождений заданной строки)
 * с высокой степенью конкуррентности (практически без блокировок).
 *
 * После того, как построение суффиксного автомата будет завершено и все операции чтения, инициированные до завершения построения суффиксного автомата, будут выполнены,
 * все состояния BuildingTimeState будут заменены на эквивалентные им состояния DefinitiveState прозрачным для читателей образом.
 * Причем это будет сделано постепенно, без блокировки читателей.
 * См. SuffixAutomation.
 *
 * @author Shulnyaev Roman
 */
final class BuildingTimeState implements State {

    BuildingTimeState(
        int greatestSubstringLength,
        int minSubstringEndPos,
        boolean clone,
        ParallelToBuildingReadingInfo parallelToBuildReadInfo) {

        this.greatestSubstringLength = greatestSubstringLength;
        this.minSubstringEndPos = minSubstringEndPos;
        this.clone = clone;
        this.parallelToBuildReadInfo = parallelToBuildReadInfo;
    }

    @Override
    public int getGreatestSubstringLength() {
        return greatestSubstringLength;
    }

    @Override
    public int getMinSubstringEndPos() {
        return minSubstringEndPos;
    }

    @Override
    public void setSuffixLink(int targetStateId) {
        this.suffixLink = targetStateId;
    }

    @Override
    public int getSuffixLink() {
        return suffixLink;
    }

    @Override
    public void addInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep) {
        invertedSuffixLinks.put(new OrderedIntPair(sourceStateId, currentAutomationBuildStep),
                                GREATER_THAN_MAX_BUILDING_STEP);
    }

    @Override
    public void removeInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep) {
        Map<OrderedIntPair, Integer> removingInvertedSuffixLinks =
            invertedSuffixLinks.subMap(
                new OrderedIntPair(sourceStateId, LESS_THAN_MIN_BUILDING_STEP),
                new OrderedIntPair(sourceStateId, GREATER_THAN_MAX_BUILDING_STEP + 1));
        int minParallelToReadBuildStep = getMinParallelToReadingBuildingStep(currentAutomationBuildStep);

        for (OrderedIntPair removingInvertedSuffixLink : removingInvertedSuffixLinks.keySet()) {
            if (currentAutomationBuildStep > 0 && !isActualInvertedSuffixLink(removingInvertedSuffixLink, minParallelToReadBuildStep)) {
                invertedSuffixLinks.remove(removingInvertedSuffixLink);
            }
            else if (isActualInvertedSuffixLink(removingInvertedSuffixLink, currentAutomationBuildStep)) {
                invertedSuffixLinks.replace(removingInvertedSuffixLink, currentAutomationBuildStep);
            }
        }
    }

    private int getMinParallelToReadingBuildingStep(int currentAutomationBuildStep) {
        int minParallelToReadBuildStep = parallelToBuildReadInfo.minLastVisibleToReadBuildStep;

        if (minParallelToReadBuildStep == LESS_THAN_MIN_BUILDING_STEP) {
            minParallelToReadBuildStep = currentAutomationBuildStep - 1;
        }

        return minParallelToReadBuildStep;
    }

    @Override
    public Iterator<Integer> getActualInvertedSuffixLinksIterator(int automationBuildStep) {
        return new ActualInvertedSuffixLinksIterator(automationBuildStep);
    }

    @Override
    public int getActualInvertedSuffixLinkCount(int automationBuildStep) {
        int actualInvertedSuffixLinkCount = 0;

        for (OrderedIntPair invertedSuffixLink : invertedSuffixLinks.keySet()) {
            if (isActualInvertedSuffixLink(invertedSuffixLink, automationBuildStep)) {
                ++actualInvertedSuffixLinkCount;
            }
        }

        return actualInvertedSuffixLinkCount;
    }

    @Override
    public boolean hasActualTransition(char transitionChar, int automationBuildStep) {
        return getActualTransitionTargetStateId(transitionChar, automationBuildStep) != NO_STATE_ID;
    }

    @Override
    public void addTransition(char transitionChar, int targetStateId, int currentAutomationBuildStep) {
        transitions.put(
            new OrderedCharIntPair(transitionChar, currentAutomationBuildStep),
            new BuildingTimeTransition(targetStateId, GREATER_THAN_MAX_BUILDING_STEP));
    }

    @Override
    public void redirectTransition(char transitionChar, int newTargetStateId, int currentAutomationBuildStep) {
        removeTransition(transitionChar, currentAutomationBuildStep);
        addTransition(transitionChar, newTargetStateId, currentAutomationBuildStep);
    }

    private void removeTransition(char transitionChar, int currentAutomationBuildStep) {
        Map<OrderedCharIntPair, BuildingTimeTransition> removingTransitions = getAllTransitions(transitionChar);
        int minParallelToReadBuildStep = getMinParallelToReadingBuildingStep(currentAutomationBuildStep);

        for (OrderedCharIntPair removingTransition : removingTransitions.keySet()) {
            if (currentAutomationBuildStep > 0 && !isActualTransition(removingTransition, minParallelToReadBuildStep)) {
                transitions.remove(removingTransition);
            }
            else if (isActualTransition(removingTransition, currentAutomationBuildStep)) {
                transitions.replace(
                    removingTransition,
                    new BuildingTimeTransition(
                            removingTransitions.get(removingTransition).targetStateId,
                            currentAutomationBuildStep));
            }
        }
    }

    private ConcurrentNavigableMap<OrderedCharIntPair, BuildingTimeTransition> getAllTransitions(char transitionChar) {
        return
            transitions.subMap(
                new OrderedCharIntPair(transitionChar, LESS_THAN_MIN_BUILDING_STEP),
                new OrderedCharIntPair(transitionChar, GREATER_THAN_MAX_BUILDING_STEP + 1));
    }

    @Override
    public int getActualTransitionTargetStateId(char transitionChar, int automationBuildStep) {
        for (OrderedCharIntPair inputCharTransition : getAllTransitions(transitionChar).keySet()) {
            if (isActualTransition(inputCharTransition, automationBuildStep)) {
                return transitions.get(inputCharTransition).targetStateId;
            }
        }

        return NO_STATE_ID;
    }

    private boolean isActualTransition(OrderedCharIntPair transition, int automationBuildStep) {
        int firstIrrelevantAutomationBuildStep = transitions.get(transition).firstIrrelevantBuildStep;
        return automationBuildStep >= transition.right && automationBuildStep < firstIrrelevantAutomationBuildStep;
    }

    @Override
    public ActualTransitionsIterator getActualTransitionsIterator(int automationBuildStep) {
        return new ActualTransitionsIterator(automationBuildStep);
    }

    @Override
    public int getActualTransitionCount(int automationBuildStep) {
        int actualTransitionCount = 0;

        for (OrderedCharIntPair transition : transitions.keySet()) {
            if (isActualTransition(transition, automationBuildStep)) {
                ++actualTransitionCount;
            }
        }

        return actualTransitionCount;
    }

    @Override
    public boolean isClone() {
        return clone;
    }

    /**
     *  Итератор по обратным суффиксным ссылкам, которые являются актуальными для данного шага построения суффиксного автомата.
     */
    private final class ActualInvertedSuffixLinksIterator implements Iterator<Integer> {

        private ActualInvertedSuffixLinksIterator(int automationBuildStep) {
            this.automationBuildStep = automationBuildStep;
            this.invertedSuffixLinksIter = invertedSuffixLinks.keySet().iterator();
            findNext();
        }

        @Override
        public boolean hasNext() {
            return nextActualInvertedSuffixLink != null;
        }

        @Override
        public Integer next() {
            if (nextActualInvertedSuffixLink != null) {
                Integer currentActualInvertedSuffixLink = nextActualInvertedSuffixLink;
                findNext();
                return currentActualInvertedSuffixLink;
            }
            else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

        // ****************************** //

        private void findNext() {
            while (invertedSuffixLinksIter.hasNext()) {
                OrderedIntPair invertedSuffixLink = invertedSuffixLinksIter.next();

                if (isActualInvertedSuffixLink(invertedSuffixLink, automationBuildStep)) {
                    nextActualInvertedSuffixLink = invertedSuffixLink.left;
                    return;
                }
            }

            nextActualInvertedSuffixLink = null;
        }

        // ****************************** //

        private final int automationBuildStep;
        private final Iterator<OrderedIntPair> invertedSuffixLinksIter;
        private Integer nextActualInvertedSuffixLink;
    }

    private boolean isActualInvertedSuffixLink(OrderedIntPair invertedSuffixLink, int automationBuildStep) {
        int firstIrrelevantAutomationBuildStep = invertedSuffixLinks.get(invertedSuffixLink);
        return automationBuildStep >= invertedSuffixLink.right && automationBuildStep < firstIrrelevantAutomationBuildStep;
    }

    /**
     *  Итератор по переходам, которые являются актуальными для данного шага построения суффиксного автомата.
     */
    private final class ActualTransitionsIterator implements Iterator<OrderedCharIntPair> {

        private ActualTransitionsIterator(int automationBuildStep) {
            this.automationBuildStep = automationBuildStep;
            this.transitionsIter = transitions.keySet().iterator();
            findNext();
        }

        @Override
        public boolean hasNext() {
            return nextActualTransition != null;
        }

        @Override
        public OrderedCharIntPair next() {
            if (nextActualTransition != null) {
                OrderedCharIntPair currentActualTransition = nextActualTransition;
                findNext();
                return currentActualTransition;
            }
            else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

        // ****************************** //

        private void findNext() {
            while (transitionsIter.hasNext()) {
                OrderedCharIntPair transition = transitionsIter.next();

                if (isActualTransition(transition, automationBuildStep)) {
                    nextActualTransition = new OrderedCharIntPair(transition.left, transitions.get(transition).targetStateId);
                    return;
                }
            }

            nextActualTransition = null;
        }

        // ****************************** //

        private final int automationBuildStep;
        private final Iterator<OrderedCharIntPair> transitionsIter;
        private OrderedCharIntPair nextActualTransition;
    }

    // ****************************** //

    // Наибольшая длина подстроки,
    // принадлежащей классу эквивалентности множеств позиций окончаний подстрок,
    // соответствующему данному состоянию суффиксного автомата.
    private final int greatestSubstringLength;

    // Минимальная позиция окончания подстроки,
    // принадлежащей классу эквивалентности множеств позиций окончаний подстрок,
    // соответствующему данному состоянию суффиксного автомата.
    private final int minSubstringEndPos;

    // Суффиксная ссылка.
    private int suffixLink;

    // Сопоставление паре
    //     (состояние-источник соответствующей прямой суффиксной ссылки;
    //      шаг построения суффиксного автомата, на котором обратная суффиксная ссылка была создана)
    // шага построения суффиксного автомата, начиная с которого читатель суффиксного автомата
    // должен считать обратную суффиксную ссылку недействительной.
    private final ConcurrentSkipListMap<OrderedIntPair, Integer> invertedSuffixLinks = new ConcurrentSkipListMap<>();

    // Сопоставление паре
    //     (символ, соответствующий переходу;
    //      шаг построения суффиксного автомата, на котором переход был создан)
    // пары
    //     (состояние-цель, в которое осуществляется переход;
    //      шаг построения суффиксного автомата, начиная с которого читатель суффиксного автомата должен считать переход недействительным).
    private final ConcurrentSkipListMap<OrderedCharIntPair, BuildingTimeTransition> transitions = new ConcurrentSkipListMap<>();

    // Истина в том и только в том случае, если данное состояние является клоном (см. SuffixAutomation) другого.
    private final boolean clone;

    // Информация о чтениях суффиксного автомата, параллельных его построению.
    private final ParallelToBuildingReadingInfo parallelToBuildReadInfo;
}
