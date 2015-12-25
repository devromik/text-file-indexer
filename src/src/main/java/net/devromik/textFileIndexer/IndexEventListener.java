package net.devromik.textFileIndexer;

import java.nio.file.Path;

/**
 * Реализация должна быть потоково-безопасной.
 *
 * @author Shulnyaev Roman
 */
public interface IndexEventListener {

    /**
     * @throws java.lang.NullPointerException если sourceFilePath == null.
     */
    void onIndexBuildingStarted(Path sourceFilePath);

    /**
     * @throws java.lang.NullPointerException если sourceFilePath == null.
     */
    void onIndexBuildingCancelled(Path sourceFilePath);

    /**
     * @throws java.lang.NullPointerException если sourceFilePath == null.
     */
    void onIndexBuildingSuccessfullyCompleted(Path sourceFilePath);

    /**
     * @throws java.lang.NullPointerException если sourceFilePath == null || indexBuildingException == null.
     */
    void onIndexBuildingErrorOccurred(Path sourceFilePath, Exception indexBuildingException);
}
