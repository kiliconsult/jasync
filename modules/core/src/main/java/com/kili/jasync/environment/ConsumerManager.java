package com.kili.jasync.environment;

import com.kili.jasync.Consumer;
import com.kili.jasync.fail.FailedItem;
import com.kili.jasync.transaction.Transaction;
import com.kili.jasync.transaction.TransactionFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsumerManager {

   private TransactionFactory transactionFactory;
   private final ThreadPoolExecutor executorService;

   public ConsumerManager(TransactionFactory transactionFactory, int maxConsumers) {
      this.transactionFactory = transactionFactory;
      executorService = new ThreadPoolExecutor(
            1,
            maxConsumers,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(5 * maxConsumers));
   }

   public <T> void offerConsumers(Consumer<T> consumer, T workItem) {
      executorService.submit(() -> {

         Transaction transaction = transactionFactory.initTransaction();
         try (transaction) {
            consumer.consume(workItem);
            transaction.commit();
         } catch (Exception e) {
            try {
               transaction.rollback();
               consumer.handleUncaughtException(new FailedItem<>(workItem, e));
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
}
