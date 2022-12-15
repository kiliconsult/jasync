package com.kili.jasync.environment.memory;

import com.kili.jasync.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.environment.ConsumerConfiguration;
import com.kili.jasync.environment.ConsumerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MemoryAsyncEnvironment implements AsyncEnvironment {

   private static final Logger logger = LoggerFactory.getLogger(MemoryAsyncEnvironment.class);
   private Map<Class<? extends Consumer>, MemoryWorker<?>> workers = new HashMap<>();

   @Override
   public void initializeWorker(Consumer<?> worker, ConsumerConfiguration configuration) {
      logger.info("Initializing worker {}", worker);

      var consumerManager = new ConsumerManager(configuration.getTransactionFactory(), configuration.getMaxWorkers());

      var memoryWorker = new MemoryWorker<>(worker, consumerManager);
      workers.put(worker.getClass(), memoryWorker);
      new Thread(memoryWorker).start();
   }

   @Override
   public <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException {
      var memoryWorker = (MemoryWorker<T>) workers.get(workerType);
      if (memoryWorker == null) {
         throw new JAsyncException("Unknown worker, " + workerType);
      }
      memoryWorker.queueWorkItem(workItem);
   }
}
