package com.kili.jasync;

import com.kili.jasync.fail.FailedItem;

/**
 * Stateless consumer
 * @param <T> the type of messages being handled by the consumer
 */
public interface Consumer<T> {

   void consume(T workItem);

   default void handleUncaughtException(FailedItem<T> failedItem) {

   }
}
