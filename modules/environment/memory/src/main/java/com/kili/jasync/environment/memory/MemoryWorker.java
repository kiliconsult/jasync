package com.kili.jasync.environment.memory;

import com.kili.jasync.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

class MemoryWorker<T> implements Runnable {


   private static Logger logger = LoggerFactory.getLogger(MemoryWorker.class);
   private Consumer<T> worker;
   private ConsumerManager consumerManager;
   private LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();

   public MemoryWorker(Consumer<T> worker, ConsumerManager consumerManager) {
      this.worker = worker;
      this.consumerManager = consumerManager;
   }

   public void queueWorkItem(T workItem) {
      queue.add(workItem);
   }

   @Override
   public void run() {
      try {
         while (true) {
            var workItem = queue.peek();
            if (workItem != null) {
               consumerManager.offerConsumers(worker, workItem);
               queue.remove();
            }
         }
      } catch (RejectedExecutionException e) {
         try {
            Thread.sleep(10);
         } catch (InterruptedException ex) {
            logger.warn("Interrupted!. Stopping worker");
         }
      }
   }

   public void close() {
      consumerManager.close();
   }
}
