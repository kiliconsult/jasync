package com.kili.jasync.environment.memory;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.consumer.WorkerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment that only lives in memory. Hence, this environment can only be used internally in one JVM and not to
 * send messages to other systems.
 *
 * Anything that lives inside this environment will be forgotten when the JVM closes.
 */
public class MemoryAsyncEnvironment implements AsyncEnvironment {

   private static final Logger logger = LoggerFactory.getLogger(MemoryAsyncEnvironment.class);
   private Map<Class<? extends Consumer>, MemoryWorker<?>> workers = new HashMap<>();

   @Override
   public <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, WorkerConfiguration configuration) {
      logger.info("Initializing memory worker {}", worker);

      var consumerManager = new ConsumerManager<>(worker, configuration.getNumberOfConsumers());

      var memoryWorker = new MemoryWorker<>(worker, consumerManager);
      workers.put(worker.getClass(), memoryWorker);
      Thread thread = new Thread(memoryWorker);
      thread.start();
   }

   @Override
   public <T> void initializeMessageHandler(
         Consumer<T> worker,
         Class<T> itemClass,
         MessageHandlerConfiguration configuration) {
      throw new UnsupportedOperationException("Not yet supported!");
   }

   @Override
   public <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException {
      var memoryWorker = (MemoryWorker<T>) workers.get(workerType);
      if (memoryWorker == null) {
         throw new JAsyncException(workerType + " is not registered as a worker!");
      }
      memoryWorker.queueWorkItem(workItem);
   }

   @Override
   public <T> void sendRoutedMessage(String route, T message) {
      throw new UnsupportedOperationException("Not yet supported!");
   }

   @Override
   public QueueInfo getQueueInfo(Class<? extends Consumer<?>> consumerType) throws JAsyncException {
      var consumer = (MemoryWorker<?>) workers.get(consumerType);
      if (consumer == null) {
         throw new JAsyncException(consumer + " is not registered!");
      }
      return new QueueInfo(consumer.getQueueSize());
   }

   @Override
   public void close() {
      for (MemoryWorker<?> worker : workers.values()) {
         worker.close();
      }
      workers.clear();
   }
}
