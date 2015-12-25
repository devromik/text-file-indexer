package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.util.Iterator;
import net.devromik.textFileIndexer.utils.OrderedCharIntPair;

/**
 * Состояние суффиксного автомата.
 * Биективно соответствует классу эквивалентности множеств позиций окончаний подстрок индексируемой строки.
 * Идентифицируется неотрицательным целым числом (см. SuffixAutomation).
 */
interface State {

    int NO_STATE_ID = -1;

    // ****************************** //

    /**
     * @return наибольшую длину подстроки,
     *         принадлежащей классу эквивалентности множеств позиций окончаний подстрок,
     *         соответствующему данному состоянию суффиксного автомата.
     */
    int getGreatestSubstringLength();

    /**
     * @return минимальную позицию окончания подстроки,
     *         принадлежащей классу эквивалентности множеств позиций окончаний подстрок,
     *         соответствующему данному состоянию суффиксного автомата.
     */
    int getMinSubstringEndPos();

    void setSuffixLink(int targetStateId);
    int getSuffixLink();

    void addInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep);
    void removeInvertedSuffixLink(int sourceStateId, int currentAutomationBuildStep);
    Iterator<Integer> getActualInvertedSuffixLinksIterator(int automationBuildStep);
    int getActualInvertedSuffixLinkCount(int automationBuildStep);

    boolean hasActualTransition(char transitionChar, int automationBuildStep);
    void addTransition(char transitionChar, int targetStateId, int currentAutomationBuildStep);
    void redirectTransition(char transitionChar, int newTargetStateId, int currentAutomationBuildStep);
    int getActualTransitionTargetStateId(char transitionChar, int automationBuildStep);
    Iterator<OrderedCharIntPair> getActualTransitionsIterator(int automationBuildStep);
    int getActualTransitionCount(int automationBuildStep);

    boolean isClone();
}