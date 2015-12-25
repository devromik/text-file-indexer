package net.devromik.textFileIndexer.impl.suffixAutomation;

/**
 * @author Shulnyaev Roman
 */
final class BuildingTimeTransition {

    /**
     * @param firstIrrelevantBuildStep шаг построения суффиксного автомата, начиная с которого данный переход считается недействительным.
     */
    BuildingTimeTransition(int targetStateId, int firstIrrelevantBuildStep) {
        this.targetStateId = targetStateId;
        this.firstIrrelevantBuildStep = firstIrrelevantBuildStep;
    }

    // ****************************** //

    final int targetStateId;

    // Шаг построения суффиксного автомата, начиная с которого данный переход считается недействительным.
    final int firstIrrelevantBuildStep;
}
