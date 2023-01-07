package com.kili.jasync.environment.memory;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

class MemoryWorker<T> implements Runnable {


   private static Logger logger = LoggerFactory.getLogger(MemoryWorker.class);
   private final Consumer<T> worker;
   private final ConsumerManager<T> consumerManager;
   private final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();
   private boolean closed;

   public MemoryWorker(Consumer<T> worker, ConsumerManager<T> consumerManager) {
      this.worker = worker;
      this.consumerManager = consumerManager;
   }

   public void queueWorkItem(T workItem) throws JAsyncException {
      if (closed) {
         throw new JAsyncException("Worker is closed!");
      }
      queue.add(workItem);
   }

   @Override
   public void run() {
      runFeeder();
   }

   private void runFeeder() {
      try {
         while (true) {
            var workItem = queue.peek();
            if (workItem != null) {
               consumerManager.offerConsumers(workItem);
               queue.remove();
            } else {
               Thread.sleep(10);
            }
         }
      } catch (RejectedExecutionException e) {
         try {
            Thread.sleep(10);
            runFeeder();
         } catch (InterruptedException ex) {
            logger.warn("Interrupted!. Stopping feeder");
         }
      } catch (InterruptedException e) {
         logger.warn("Interrupted!. Stopping feeder");
      }
   }

   public int getQueueSize() {
      return queue.size();
   }

   public void close() {
      closed = true;
      consumerManager.close();

   }
}
