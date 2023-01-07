package com.kili.jasync.consumer;

import com.kili.jasync.fail.FailedItem;

/**
 * Stateless consumer
 * @param <T> the type of messages being handled by the consumer
 */
public interface Consumer<T> {

   void consume(T workItem) throws Exception;

   default void handleUncaughtException(FailedItem<T> failedItem) {
      // ignore
   }
}
