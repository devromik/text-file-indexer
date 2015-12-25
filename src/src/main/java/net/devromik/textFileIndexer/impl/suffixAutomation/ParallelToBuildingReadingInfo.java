package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Shulnyaev Roman
 */
class ParallelToBuildingReadingInfo {

    void onParallelToBuildingReadingStarted(int lastVisibleToReadBuildStep) {
        ++totalReadCount;

        if (lastVisibleToReadBuildStepToReadCount.containsKey(lastVisibleToReadBuildStep)) {
            lastVisibleToReadBuildStepToReadCount.put(
                lastVisibleToReadBuildStep,
                lastVisibleToReadBuildStepToReadCount.get(lastVisibleToReadBuildStep) + 1);
        }
        else {
            lastVisibleToReadBuildStepToReadCount.put(lastVisibleToReadBuildStep, 1);
        }

        if (lastVisibleToReadBuildStepToReadCount.firstKey() > minLastVisibleToReadBuildStep) {
            minLastVisibleToReadBuildStep = lastVisibleToReadBuildStepToReadCount.firstKey();
        }
    }

    void onParallelToBuildingReadingCompleted(int lastVisibleToReadBuildStep) {
        --totalReadCount;
        lastVisibleToReadBuildStepToReadCount.put(
            lastVisibleToReadBuildStep,
            lastVisibleToReadBuildStepToReadCount.get(lastVisibleToReadBuildStep) - 1);

        if (lastVisibleToReadBuildStepToReadCount.get(lastVisibleToReadBuildStep) == 0) {
            lastVisibleToReadBuildStepToReadCount.remove(lastVisibleToReadBuildStep);
        }

        if (!lastVisibleToReadBuildStepToReadCount.isEmpty() &&
            lastVisibleToReadBuildStepToReadCount.firstKey() > minLastVisibleToReadBuildStep) {

            minLastVisibleToReadBuildStep = lastVisibleToReadBuildStepToReadCount.firstKey();
        }
    }

    // ****************************** //

    int totalReadCount;
    NavigableMap<Integer, Integer> lastVisibleToReadBuildStepToReadCount = new ConcurrentSkipListMap<>();
    volatile int minLastVisibleToReadBuildStep = SuffixAutomation.LESS_THAN_MIN_BUILDING_STEP;
}
