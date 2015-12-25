package net.devromik.textFileIndexer.impl.suffixAutomation;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.slf4j.*;
import static java.lang.Thread.*;
import static java.text.MessageFormat.format;
import net.devromik.textFileIndexer.OccurrencePosIterator;
import net.devromik.textFileIndexer.impl.EmptyOccurrencePosIterator;
import static net.devromik.textFileIndexer.impl.suffixAutomation.MainMemorySuffixAutomationIndex.MAX_ACCEPTABLE_SOURCE_FILE_LENGTH;
import net.devromik.textFileIndexer.utils.*;
import static org.slf4j.LoggerFactory.*;

/**
 * Суффиксный автомат.
 * Реализация основана на этой статье: http://e-maxx.ru/algo/suffix_automata.
 *
 * @author Shulnyaev Roman
 */
final class SuffixAutomation {

    static final int INITIAL_STATE_ARRAY_CAPACITY = 1024;
    static final int LESS_THAN_MIN_BUILDING_STEP = -1;
    static final int GREATER_THAN_MAX_BUILDING_STEP = MAX_ACCEPTABLE_SOURCE_FILE_LENGTH + 1;

    // ****************************** //

    SuffixAutomation(Path sourceFilePath) {
        checkSetSourceFilePath(sourceFilePath);
        initParallelToBuildReadInfo();
        createHeadState();
        initPostbuildOptimizingStatus();
    }

    void extend(char currentChar) {
        growStateArrayIfNeeded();

        // Создаем новое состояние, соответствующее прочитанному префиксу readPrefix исходной строки (включая currentChar).
        int newStateId = stateCount++;
        int currentBuildStep = newStateId;
        int newStateGreatestSubstringLength = states.get(lastOriginStateId).getGreatestSubstringLength() + 1;
        State newState =
            new BuildingTimeState(
                    newStateGreatestSubstringLength,
                    newStateGreatestSubstringLength - 1,
                    false,
                    parallelToBuildReadInfo);

        // Добавляем новое состояние в общий набор состояний.
        states.set(newStateId, newState);

        // Двигаемся вверх по суффиксным ссылкам, начиная с состояния lastOriginStateId, соответствующего прочитанному префиксу prevReadPrefix исходной строки без currentChar в конце.
        // Если текущее состояние stateId не имеет перехода по currentChar, то добавляем для этого состояния переход по currentChar.
        // Мы должны сделать это, поскольку stateId соответствует некоторому суффиксу prevReadPrefix, "расширяющему" класс эквивалентности множеств позиций окончаний подстрок,
        // соответсвующий предыдущему состоянию на нашем пути.
        // Если же stateId имеет переход по currentChar, то завершаем указанное движение (почему - см. ниже).
        int stateId = lastOriginStateId;
        State state = states.get(stateId);

        while (stateId != State.NO_STATE_ID && !state.hasActualTransition(currentChar, currentBuildStep)) {
            state.addTransition(currentChar, newStateId, currentBuildStep);
            stateId = state.getSuffixLink();
            state = stateId != State.NO_STATE_ID ? states.get(stateId) : null;
        }

        // Если при движении по суффиксным ссылкам мы не встретили состояний, которые имеют переход по currentChar, то currentChar ранее в строке не втречался.
        // В этом случае мы успешно добавили все переходы, осталось только проставить суффиксную ссылку у состояния newState -
        // она, очевидно, должна указывать на начальное состояние, поскольку состоянию newState соответствуют все суффиксы readPrefix.
        if (stateId == State.NO_STATE_ID) {
            setSuffixLink(newStateId, HEAD_STATE_ID, currentBuildStep);
        }
        // Если же мы встретили состояние, которое уже имеет переход по currentChar,
        // то это означает, что мы пытались добавить в автомат строку X + currentChar
        // (где X - некоторый суффикс строки prevReadPrefix, имеющий длину state.getGreatestSubstringLength()),
        // а эта строка уже была ранее добавлена в автомат (т. е. строка X + currentChar уже входит как подстрока в строку prevReadPrefix).
        // Поскольку мы предполагаем, что автомат для строки prevReadPrefix построен корректно, то новых переходов мы добавлять не должны.
        // Однако, возникает сложность с тем, куда вести суффиксную ссылку из состояния newState.
        // Нам требуется провести суффиксную ссылку в такое состояние suffixLink, в соответствующем которому классе эквивалентности наибольшей строкой будет X + currentChar,
        // т. е. suffixLink.getGreatestSubstringLength() == state.getGreatestSubstringLength() + 1.
        // Однако такого состояния могло и не существовать: в таком случае нам надо выполнить "расщепление" состояния.
        else {
            int targetId = state.getActualTransitionTargetStateId(currentChar, currentBuildStep);
            State target = states.get(targetId);

            // Если переход (state, target) оказался сплошным (target.getGreatestSubstringLength() == state.getGreatestSubstringLength() + 1),
            // то необходимости в расщеплении нет, и мы просто просто проводим суффиксную ссылку из newState в target.
            if (target.getGreatestSubstringLength() == state.getGreatestSubstringLength() + 1) {
                setSuffixLink(newStateId, targetId, currentBuildStep);
            }
            // Другой, более сложный вариант - когда переход несплошной, т. е. target.getGreatestSubstringLength() > state.getGreatestSubstringLength() + 1.
            // Это означает, что состоянию target соответствует не только нужная нам подстрока X + currentChar, но и подстроки большей длины.
            // В этом случае необходимо выполнить расщепление состояния target: разбить соответствующий ей отрезок строк на два подотрезка так,
            // чтобы длина наибольшей строки в первом из них была равна state.getGreatestSubstringLength() + 1.
            // Это можно сделать путем "клонирования" состояния target, делая его копию targetClone такой,
            // чтобы targetClone.getGreatestSubstringLength() == state.getGreatestSubstringLength() + 1.
            // Затем, копируем в targetClone из target все переходы,
            // поскольку мы не хотим менять пути, проходившие через target.
            // Суффиксную ссылку из targetClone мы ведем туда, куда вела старая суффиксная ссылка из target,
            // а ссылку из target направляем в targetClone.
            // После клонирования мы проводим суффиксную ссылку из newState в targetClone — то, ради чего мы и производили клонирование.
            // Остался последний шаг — перенаправить некоторые входящие в target переходы, перенаправив их на targetClone.
            // Какие именно входящие переходы надо перенаправить?
            // Достаточно перенаправить только переходы, соответствующие всем суффиксам строки X + currentChar,
            // т.е. нам надо продолжить двигаться по суффиксным ссылкам, начиная с состояния state, и до тех пор,
            // пока мы не дойдем до фиктивного состояния NO_STATE_ID или не дойдем до состояния,
            // переход из которого ведет в состояние, отличное от target.
            else {
                int targetCloneId = stateCount++;
                State targetClone =
                    new BuildingTimeState(
                            state.getGreatestSubstringLength() + 1,
                            target.getMinSubstringEndPos(),
                            true,
                            parallelToBuildReadInfo);
                states.set(targetCloneId, targetClone);
                setSuffixLink(targetCloneId, target.getSuffixLink(), currentBuildStep);
                Iterator<OrderedCharIntPair> targetActualTransitionsIter = target.getActualTransitionsIterator(currentBuildStep);

                while (targetActualTransitionsIter.hasNext()) {
                    OrderedCharIntPair targetActualTransition = targetActualTransitionsIter.next();
                    targetClone.addTransition(
                        targetActualTransition.left,
                        targetActualTransition.right,
                        currentBuildStep);
                }

                while (stateId != State.NO_STATE_ID && state.getActualTransitionTargetStateId(currentChar, currentBuildStep) == targetId) {
                    state.redirectTransition(currentChar, targetCloneId, currentBuildStep);
                    stateId = state.getSuffixLink();
                    state = stateId != State.NO_STATE_ID ? states.get(stateId) : null;
                }

                setSuffixLink(newStateId, targetCloneId, currentBuildStep);
                states.get(target.getSuffixLink()).removeInvertedSuffixLink(targetId, currentBuildStep);
                setSuffixLink(targetId, targetCloneId, currentBuildStep);
            }
        }

        lastOriginStateId = newStateId;
        lastVisibleToReadStateId = stateCount;
    }

    void onBuildCompleted() {
        // "Мгновенная" fine-grained блокировка (удерживается пренебрежимо малое время).
        synchronized (postbuildOptimizingMon) {
            buildCompleted = true;
            startPostbuildOptimizing();
        }
    }

    void startPostbuildOptimizing() {
        // "Мгновенная" fine-grained блокировка (удерживается пренебрежимо малое время).
        synchronized (postbuildOptimizingMon) {
            if (!buildCompleted) {
                return;
            }

            if (postbuildOptimizingCompleted) {
                postbuildOptimizingStatus = PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_COMPLETED;
                return;
            }

            if (postbuildOptimizingStatus != PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_NOT_STARTED && postbuildOptimizingStatus != PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_STOPPED) {
                return;
            }

            if (parallelToBuildReadInfo.totalReadCount > 0) {
                return;
            }

            postbuildOptimizingStatus = PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_STARTED;
            startPostbuildOptimizingThread();
        }
    }

    void stopPostbuildOptimizing() {
        // "Мгновенная" fine-grained блокировка (удерживается пренебрежимо малое время).
        synchronized (postbuildOptimizingMon) {
            if (postbuildOptimizingCompleted) {
                postbuildOptimizingStatus = PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_COMPLETED;
                return;
            }

            if (postbuildOptimizingStatus == PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_STARTED) {
                stopPostbuildOptimizingThread();
            }
        }
    }

    // ****************************** //

    private static final int HEAD_STATE_ID = 0;

    // ****************************** //

    private void checkSetSourceFilePath(Path sourceFilePath) {
        this.sourceFilePath = PreconditionUtils.checkNotNull(sourceFilePath);
    }

    private void createHeadState() {
        State headState = new BuildingTimeState(0, State.NO_STATE_ID, false, parallelToBuildReadInfo);
        headState.setSuffixLink(State.NO_STATE_ID);
        states.set(0, headState);
        stateCount = 1;
    }

    private State getHeadState() {
        return states.get(HEAD_STATE_ID);
    }

    private void growStateArrayIfNeeded() {
        // Поскольку за одну операцию расширения суффиксного автомата к нему добавляется не более двух состояний,
        // расширяем массив состояний в том и только в том случае, когда stateCount > states.length() - 2.
        if (stateCount > states.length() - 2) {
            states = ArrayUtils.growAtomicReferenceArrayIfNeeded(states, stateCount + 2);
        }
    }

    private void setSuffixLink(int sourceStateId, int targetStateId, int buildStep) {
        states.get(sourceStateId).setSuffixLink(targetStateId);
        states.get(targetStateId).addInvertedSuffixLink(sourceStateId, buildStep);
    }

    /* ***** Чтение (поиск всех вхождений заданной строки в исходную строку). ***** */

    /**
     * Ленивый итератор по всем вхождениям заданной строки в исходную строку.
     */
    private class SuffixAutomationOccurrencePosIterator implements OccurrencePosIterator {

        private SuffixAutomationOccurrencePosIterator(
            int lastVisibleToReadBuildStep,
            String soughtForCharSeq,
            State soughtForCharSeqState) {

            this.lastVisibleToReadBuildStep = lastVisibleToReadBuildStep;
            this.soughtForCharSeq = soughtForCharSeq;
            this.dfsQueue = new LinkedList<>();
            this.dfsQueue.add(soughtForCharSeqState);

            // "Мгновенная" fine-grained блокировка (удерживается пренебрежимо малое время).
            synchronized (postbuildOptimizingMon) {
                if (buildCompleted) {
                    parallelToBuildRead = false;
                }
                else {
                    parallelToBuildRead = true;
                    parallelToBuildReadInfo.onParallelToBuildingReadingStarted(lastVisibleToReadBuildStep);
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !dfsQueue.isEmpty();
        }

        @Override
        public long getNext() {
            checkNotClosed();
            PreconditionUtils.checkState(hasNext());
            long currentOccurrencePos = -1;

            while (!dfsQueue.isEmpty()) {
                State currentState = dfsQueue.poll();
                Iterator<Integer> currentStateActualInvertedLinksIter = currentState.getActualInvertedSuffixLinksIterator(lastVisibleToReadBuildStep);

                while (currentStateActualInvertedLinksIter.hasNext()) {
                    dfsQueue.add(states.get(currentStateActualInvertedLinksIter.next()));
                }

                if (!currentState.isClone()) {
                    currentOccurrencePos = currentState.getMinSubstringEndPos() - soughtForCharSeq.length() + 1;

                    while (!dfsQueue.isEmpty()) {
                        currentState = dfsQueue.poll();

                        if (!currentState.isClone()) {
                            dfsQueue.addFirst(currentState);
                            break;
                        }
                        else {
                            currentStateActualInvertedLinksIter = currentState.getActualInvertedSuffixLinksIterator(lastVisibleToReadBuildStep);

                            while (currentStateActualInvertedLinksIter.hasNext()) {
                                dfsQueue.add(states.get(currentStateActualInvertedLinksIter.next()));
                            }
                        }
                    }

                    if (dfsQueue.isEmpty()) {
                        close();
                    }

                    break;
                }
            }

            return currentOccurrencePos;
        }

        @Override
        public void close() {
            checkNotClosed();
            dfsQueue.clear();

            // "Мгновенная" fine-grained блокировка (удерживается пренебрежимо малое время).
            synchronized (postbuildOptimizingMon) {
                if (parallelToBuildRead) {
                    parallelToBuildReadInfo.onParallelToBuildingReadingCompleted(lastVisibleToReadBuildStep);
                    startPostbuildOptimizing();
                }
            }

            closed = true;
        }

        // ****************************** //

        private void checkNotClosed() {
            PreconditionUtils.checkState(!closed);
        }

        // ****************************** //

        private final int lastVisibleToReadBuildStep;
        private final String soughtForCharSeq;
        private final Deque<State> dfsQueue;
        private final boolean parallelToBuildRead;
        private boolean closed;
    }

    // ****************************** //

    OccurrencePosIterator getOccurrencePosIterator(String soughtForCharSeq) {
        int lastVisibleToReadBuildStep = lastVisibleToReadStateId;
        State soughtForCharSeqState = findStateForSoughtForCharSequence(soughtForCharSeq, lastVisibleToReadBuildStep);

        if (soughtForCharSeqState == null) {
            return EmptyOccurrencePosIterator.INSTANCE;
        }

        return new SuffixAutomationOccurrencePosIterator(lastVisibleToReadBuildStep, soughtForCharSeq, soughtForCharSeqState);
    }

    private State findStateForSoughtForCharSequence(String soughtForCharSeq, int lastVisibleToReadBuildStep) {
        if (StringUtils.isNullOrEmpty(soughtForCharSeq)) {
            return null;
        }

        State currentState = getHeadState();

        for (int i = 0; i < soughtForCharSeq.length(); ++i) {
            char currentChar = soughtForCharSeq.charAt(i);
            int nextStateId = currentState.getActualTransitionTargetStateId(currentChar, lastVisibleToReadBuildStep);

            if (nextStateId != State.NO_STATE_ID) {
                currentState = states.get(nextStateId);
            }
            else {
                return null;
            }
        }

        return currentState;
    }

    /* ***** Оптимизация после построения (и завершения всех параллельных построению операций чтения). ***** */

    private static final String POSTBUILD_OPTIMIZATION_THREAD_NAME_PATTERN = "Suffix Automation Postbuild Optimizer [source file pathname = \"{0}\"]";

    // ****************************** //

    private class PostbuildOptimization implements Runnable {

        @Override
        public void run() {
            while (!postbuildOptimizingCompleted && !interrupted()) {
                try {
                    if (globalInvertedSuffixLinkArray == null) {
                        // Расширяем постепенно, поскольку в этом случае, учитывая одновременное удаление состояний времени выполнения,
                        // мы сможем достичь гораздо меньшего пикового потребления памяти,
                        // чем в том случае, когда мы установили бы изначальную емкость равной stateCount (гарантированный инфимум).
                        globalInvertedSuffixLinkArray = new int[256];
                        globalTransitionCharArray = new char[256];
                        globalTransitionTargetStateIdArray = new int[256];
                    }

                    postbuildOptimizeNextState();

                    if (postbuildOptimizedStateCount == stateCount) {
                        postbuildOptimizingCompleted = true;
                    }
                }
                catch (Exception exception) {
                    Slf4jUtils.logException(logger, exception);

                    try {
                        sleep(MainMemorySuffixAutomationIndexBuilder.ERROR_TIMEOUT_IN_MILLIS);
                    }
                    catch (InterruptedException interruptedException) {
                        return;
                    }
                }
            }
        }
    }

    // ****************************** //

    private void initPostbuildOptimizingStatus() {
        postbuildOptimizingStatus = PostbuildOptimizingStatus.POSTBUILD_OPTIMIZING_NOT_STARTED;
    }

    private void initParallelToBuildReadInfo() {
        parallelToBuildReadInfo = new ParallelToBuildingReadingInfo();
    }

    private String makePostbuildOptimizingThreadName() {
        return format(POSTBUILD_OPTIMIZATION_THREAD_NAME_PATTERN, sourceFilePath.toString());
    }

    private void startPostbuildOptimizingThread() {
        postbuildOptimizingThread = new Thread(new PostbuildOptimization(), makePostbuildOptimizingThreadName());
        postbuildOptimizingThread.start();
    }

    private void stopPostbuildOptimizingThread() {
        postbuildOptimizingThread.interrupt();

        try {
            logger.info("Waiting for postbuild optimizing thread \"{}\" stopped...", makePostbuildOptimizingThreadName());
            postbuildOptimizingThread.join();
            logger.info("Postbuild optimizing thread \"{}\" successfully stopped", makePostbuildOptimizingThreadName());
        }
        catch (InterruptedException exception) {
            logger.error("Interrupted while waiting for postbuild optimizing thread \"{}\" stopped", makePostbuildOptimizingThreadName());
        }
        finally {
            postbuildOptimizingThread = null;
        }
    }

    private void postbuildOptimizeNextState() {
        growGlobalInvertedSuffixLinkArrayIfNeeded();
        growGlobalTransitionArrayIfNeeded();

        State buildingTimeState = states.get(postbuildOptimizedStateCount);
        DefinitiveState definitiveState =
            new DefinitiveState(
                    buildingTimeState,
                    globalInvertedSuffixLinkArray,
                    globalInvertedSuffixLinkArrayFirstFreePos,
                    globalTransitionCharArray,
                    globalTransitionTargetStateIdArray,
                    globalTransitionArrayFirstFreePos,
                    lastVisibleToReadStateId);
        states.set(postbuildOptimizedStateCount, definitiveState);

        globalInvertedSuffixLinkArrayFirstFreePos = definitiveState.getGlobalInvertedSuffixLinkArrayFollowingLastPos();
        globalTransitionArrayFirstFreePos = definitiveState.getGlobalTransitionArrayFollowingLastPos();
        ++postbuildOptimizedStateCount;
    }

    private void growGlobalInvertedSuffixLinkArrayIfNeeded() {
        int nextStateActualInvertedSuffixLinkCount = states.get(postbuildOptimizedStateCount).getActualInvertedSuffixLinkCount(lastVisibleToReadStateId);
        int globalInvertedSuffixLinkArrayFreeCapacity = globalInvertedSuffixLinkArray.length - globalInvertedSuffixLinkArrayFirstFreePos;

        if (globalInvertedSuffixLinkArrayFreeCapacity < nextStateActualInvertedSuffixLinkCount) {
            globalInvertedSuffixLinkArray =
                ArrayUtils.growIntArrayIfNeeded(
                    globalInvertedSuffixLinkArray,
                    globalInvertedSuffixLinkArray.length + (nextStateActualInvertedSuffixLinkCount - globalInvertedSuffixLinkArrayFreeCapacity));

            for (int i = 0; i < postbuildOptimizedStateCount; ++i) {
                ((DefinitiveState)states.get(i)).setGlobalInvertedSuffixLinkArray(globalInvertedSuffixLinkArray);
            }
        }
    }

    private void growGlobalTransitionArrayIfNeeded() {
        int nextStateActualTransitionCount = states.get(postbuildOptimizedStateCount).getActualTransitionCount(lastVisibleToReadStateId);
        int globalTransitionArrayFreeCapacity = globalTransitionCharArray.length - globalTransitionArrayFirstFreePos;

        if (globalTransitionArrayFreeCapacity < nextStateActualTransitionCount) {
            globalTransitionCharArray =
                ArrayUtils.growCharArrayIfNeeded(
                    globalTransitionCharArray,
                    globalTransitionCharArray.length + (nextStateActualTransitionCount - globalTransitionArrayFreeCapacity));

            for (int i = 0; i < postbuildOptimizedStateCount; ++i) {
                ((DefinitiveState)states.get(i)).setGlobalTransitionCharArray(globalTransitionCharArray);
            }

            globalTransitionTargetStateIdArray =
                ArrayUtils.growIntArrayIfNeeded(
                    globalTransitionTargetStateIdArray,
                    globalTransitionTargetStateIdArray.length + (nextStateActualTransitionCount - globalTransitionArrayFreeCapacity));

            for (int i = 0; i < postbuildOptimizedStateCount; ++i) {
                ((DefinitiveState)states.get(i)).setGlobalTransitionTargetStateIdArray(globalTransitionTargetStateIdArray);
            }
        }
    }

    // ****************************** //

    private Path sourceFilePath;

    private volatile AtomicReferenceArray<State> states = new AtomicReferenceArray<>(INITIAL_STATE_ARRAY_CAPACITY);
    private volatile int stateCount;
    private int lastOriginStateId;
    private volatile int lastVisibleToReadStateId;

    private boolean buildCompleted;

    /* ***** Оптимизация после построения (и завершения всех параллельных построению операций чтения). ***** */

    private volatile PostbuildOptimizingStatus postbuildOptimizingStatus;
    private int[] globalInvertedSuffixLinkArray;
    private int globalInvertedSuffixLinkArrayFirstFreePos;
    private char[] globalTransitionCharArray;
    private int[] globalTransitionTargetStateIdArray;
    private int globalTransitionArrayFirstFreePos;
    private int postbuildOptimizedStateCount;
    private volatile boolean postbuildOptimizingCompleted;
    private ParallelToBuildingReadingInfo parallelToBuildReadInfo;
    private Thread postbuildOptimizingThread;
    private final Object postbuildOptimizingMon = new Object();

    // Логгер.
    private static final Logger logger = getLogger(SuffixAutomation.class);
}




