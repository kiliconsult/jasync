package com.kili.jasync.environment.memory;

import com.kili.jasync.Consumer;
import com.kili.jasync.fail.FailedItem;

import java.util.concurrent.*;

public class ConsumerManager {

   private final ThreadPoolExecutor executorService;

   public ConsumerManager(int maxConsumers) {
      executorService = new ThreadPoolExecutor(
            1,
            maxConsumers,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>());
      executorService.prestartAllCoreThreads();
   }

   public <T> void offerConsumers(Consumer<T> consumer, T workItem) throws RejectedExecutionException {
      executorService.submit(() -> {

         try {
            consumer.consume(workItem);
         } catch (Exception e) {
            try {
               FailedItem<T> failedItem = new FailedItem<>(workItem, e);
               consumer.handleUncaughtException(failedItem);
            } catch (Exception ex) {
               throw new RuntimeException(ex);
            }
         }
      });
   }

   /**
    * Sets the maximum number of consumers. Does not close down current threads, but closes down threads when possible.
    *
    * @param maxNumberOfConsumers the new max number of consumers
    */
   public void updateMaxNumberOfConsumers(int maxNumberOfConsumers) {
      executorService.setMaximumPoolSize(maxNumberOfConsumers);
   }

   public void close() {
      executorService.shutdown();
   }
}
