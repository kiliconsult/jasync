package com.kili.jasync.consumer;

import com.kili.jasync.fail.FailedItem;

/**
 * Stateless consumer
 * @param <T> the type of messages being handled by the consumer
 */
public interface Consumer<T> {

   /**
    * Consumer for a message
    * @param workItem the item to consume
    * @throws Exception if an exception is thrown then the message will end up as a failedItem and should be handled in
    * the #{@link #handleUncaughtException(FailedItem)} method
    */
   void consume(T workItem) throws Exception;

   /**
    * Handle an item that failed
    * @param failedItem failed item contains the message and reason that it failed
    */
   default void handleUncaughtException(FailedItem<T> failedItem) {
      // ignore
   }
}
