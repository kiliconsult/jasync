package com.kili.jasync.environment.memory;

import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.consumer.NamedThreadFactory;
import com.kili.jasync.fail.FailedItem;

import java.util.concurrent.*;

class ConsumerManager<T> {

   private Consumer<T> consumer;
   private final ThreadPoolExecutor executorService;

   public ConsumerManager(Consumer<T> consumer, int maxConsumers) {
      this.consumer = consumer;
      executorService = new ThreadPoolExecutor(
            maxConsumers,
            maxConsumers,
            0L,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new NamedThreadFactory(consumer.getClass().getSimpleName()));
      executorService.prestartAllCoreThreads();
   }

   public void offerConsumers(T workItem) throws RejectedExecutionException {
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
