package com.kili.jasync;

import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.environment.memory.MemoryAsyncEnvironment;

public class Main {


   public static void main(String[] args) throws JAsyncException, InterruptedException {
      initialize();

      JAsyncRegistry.getEnvironment("memory").addWorkItem(ExampleConsumer.class, new WorkItem("A message!"));

      while (true) {
         Thread.sleep(1000);
         JAsyncRegistry.getEnvironment("memory").addWorkItem(ExampleConsumer.class, new WorkItem("Yet a message!"));
      }
   }

   private static void initialize() throws JAsyncException {
      AsyncEnvironment asyncEnvironment = new MemoryAsyncEnvironment();
      asyncEnvironment.initializeWorker(new ExampleConsumer(), WorkItem.class, new ConsumerConfiguration.Builder().build());
      JAsyncRegistry.registerEnvironment("memory", asyncEnvironment);
   }
}
