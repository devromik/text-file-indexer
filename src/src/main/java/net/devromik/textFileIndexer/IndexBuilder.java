package net.devromik.textFileIndexer;

import java.io.File;

/**
 * @author Shulnyaev Roman
 */
public interface IndexBuilder {
    void setSourceFile(File sourceFile);

    /**
     * @param indexEventListener должен быть потоково-безопасным.
     */
    void setIndexEventListener(IndexEventListener indexEventListener);

    /**
     * Построение индекса осуществляется асинхронно.
     * Если построение индекса уже запущено, вызов игнорируется.
     *
     * @throws java.lang.IllegalStateException в следующих случаях:
     *             - исходный файл не задан или не является корректным (см. PreconditionUtils.checkSourceFile).
     *             - не задана целевая директория (возможно, но не обязательно).
     */
    void build();

    /**
     * Отмена построения индекса осуществляется синхронно
     * (реализации должны обеспечивать константное время выполнения данной операции).
     * Вызов игнорируется в следующих случаях:
     *     - построение индекса не запущено.
     *     - отмена построения индекса уже инициирована.
     */
    void cancelBuilding();

    IndexingStatus getBuildingStatus();

    /**
     * Всегда возвращает интерактивный рабочий индекс вне зависимости от статуса построения.
     * Под интерактивностью индекса понимается его автоматическое изменение при последующем выполнении операций над построителем
     * (в том числе и в ходе выполнения таких операций).
     */
    Index getIndex();
}