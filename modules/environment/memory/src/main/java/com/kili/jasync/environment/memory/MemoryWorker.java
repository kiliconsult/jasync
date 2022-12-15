package com.kili.jasync.environment.memory;

import com.kili.jasync.Consumer;
import com.kili.jasync.environment.ConsumerManager;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

class MemoryWorker<T> implements Runnable {

   private Consumer<T> worker;
   private ConsumerManager consumerManager;
   private BlockingDeque<T> queue = new LinkedBlockingDeque<>();

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
            var workItem = queue.take();
            consumerManager.offerConsumers(worker, workItem);
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }
}
