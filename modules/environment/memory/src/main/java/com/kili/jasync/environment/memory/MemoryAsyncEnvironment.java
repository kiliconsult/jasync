package com.kili.jasync.environment.memory;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.ConsumerRegistry;
import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.consumer.WorkerConfiguration;
import com.kili.jasync.environment.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Environment that only lives in memory. Hence, this environment can only be used internally in one JVM and not to
 * send messages to other systems.
 *
 * Anything that lives inside this environment will be forgotten when the JVM closes.
 */
public class MemoryAsyncEnvironment implements AsyncEnvironment {

   private static final Logger logger = LoggerFactory.getLogger(MemoryAsyncEnvironment.class);
   private ConsumerRegistry<MemoryConsumerQueue<?>> consumerRegistry = new ConsumerRegistry<>();
   private Map<String, MemoryExchange> exchangeMap = new HashMap<>();

   @Override
   public <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, WorkerConfiguration configuration) {
      logger.info("Initializing memory worker {}", worker);

      var consumerManager = new ConsumerManager<>(worker, configuration.getNumberOfConsumers());
      var memoryWorker = new MemoryConsumerQueue<>(worker, consumerManager);

      Class<? extends Consumer<T>> aClass = (Class<? extends Consumer<T>>) worker.getClass();
      consumerRegistry.registerConsumer(aClass, itemClass, memoryWorker);

      Thread thread = new Thread(memoryWorker);
      thread.start();
   }

   @Override
   public <T> void initializeMessageHandler(
         Consumer<T> consumer,
         Class<T> itemClass,
         MessageHandlerConfiguration configuration) throws JAsyncException {
      logger.info("Initializing memory message handler {}", consumer);

      Exchange exchange = itemClass.getDeclaredAnnotation(Exchange.class);
      if (exchange == null || exchange.value() == null || exchange.value().trim().length() == 0) {
         throw new JAsyncException("Found no exchange on the message type. Expected @Exchange annotation with name of the exchange!");
      }
      String exchangeName = exchange.value();

      var consumerManager = new ConsumerManager<>(consumer, configuration.getNumberOfConsumers());
      var memoryConsumer = new MemoryConsumerQueue<>(consumer, consumerManager);

      MemoryExchange memoryExchange = exchangeMap.getOrDefault(exchangeName, new MemoryExchange(exchangeName));
      Set<String> routes = configuration.getRoutes();
      for (String route : routes) {
         memoryExchange.addRoute(route, memoryConsumer);
      }
      exchangeMap.put(exchangeName, memoryExchange);

      Class<? extends Consumer<T>> aClass = (Class<? extends Consumer<T>>) consumer.getClass();
      consumerRegistry.registerConsumer(aClass, itemClass, memoryConsumer);

      Thread thread = new Thread(memoryConsumer);
      thread.start();
   }

   @Override
   public <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException {
      Class<T> aClass = (Class<T>) workItem.getClass();
      var memoryWorker = (MemoryConsumerQueue<T>) consumerRegistry.getConsumer(workerType, aClass);
      if (memoryWorker == null) {
         throw new JAsyncException(workerType + " is not registered as a worker!");
      }
      memoryWorker.queueWorkItem(workItem);
   }

   @Override
   public <T> void sendRoutedMessage(String route, T message) throws JAsyncException {
      Exchange exchange = message.getClass().getDeclaredAnnotation(Exchange.class);
      if (exchange == null || exchange.value() == null || exchange.value().trim().length() == 0) {
         throw new JAsyncException("Found no exchange on the message. Expected @Exchange annotation with name of the exchange!");
      }

      String exchangeName = exchange.value();
      MemoryExchange memoryExchange = exchangeMap.get(exchangeName);
      if (memoryExchange != null) {
         memoryExchange.sendRoutedMessage(route, message);
      }
   }

   @Override
   public <T> QueueInfo getQueueInfo(Class<? extends Consumer<T>> consumerType, Class<T> messageType) throws JAsyncException {
      var consumer = (MemoryConsumerQueue<?>) consumerRegistry.getConsumer(consumerType, messageType);
      if (consumer == null) {
         throw new JAsyncException(consumer + " is not registered!");
      }
      return new QueueInfo(consumer.getQueueSize());
   }

   @Override
   public void close() {
      for (MemoryConsumerQueue<?> worker : consumerRegistry.values()) {
         worker.close();
      }
      consumerRegistry = new ConsumerRegistry<>();
   }
}
