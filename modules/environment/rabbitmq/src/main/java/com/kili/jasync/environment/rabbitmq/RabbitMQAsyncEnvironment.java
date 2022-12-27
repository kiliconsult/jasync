package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.environment.ConsumerConfiguration;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Environment that uses a RabbitMQ backend
 */
public class RabbitMQAsyncEnvironment implements AsyncEnvironment {

   private static final Logger logger = LoggerFactory.getLogger(RabbitMQAsyncEnvironment.class);
   private final Map<Class<? extends Consumer>, RabbitWorker<?>> workers = new HashMap<>();
   private final SerializationStrategy serializationStrategy;
   private final ConnectionFactory factory;
   private final GenericObjectPool<Channel> publishChannelPool;
   private Set<Connection> allConnections = new HashSet<>();
   private UUID uuid = UUID.randomUUID();

   public RabbitMQAsyncEnvironment(RabbitMQConfiguration rabbitMQConfiguration) throws JAsyncException {
      Connection globalPublisherConnection;
      factory = new ConnectionFactory();
      try {
         factory.setHost(rabbitMQConfiguration.getHostName());
         factory.setPort(rabbitMQConfiguration.getPort());
         factory.setUsername(rabbitMQConfiguration.getUserName());
         factory.setPassword(rabbitMQConfiguration.getPassword());
         factory.setVirtualHost(rabbitMQConfiguration.getVhost());
         globalPublisherConnection = factory.newConnection();
         allConnections.add(globalPublisherConnection);
      } catch (Exception e) {
         throw new JAsyncException("Unable to create rabbit connection", e);
      }

      publishChannelPool = new GenericObjectPool<>(new RabbitChannelFactory(globalPublisherConnection));
      publishChannelPool.setMinIdle(1);
      publishChannelPool.setMaxIdle(3);
      publishChannelPool.setMaxTotal(5);

      this.serializationStrategy = rabbitMQConfiguration.getSerializationStrategy();

      logger.info("initialized environment " + uuid);
   }

   @Override
   public <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, ConsumerConfiguration configuration) throws JAsyncException {
      logger.info("Initializing rabbit worker {}", worker);

      Channel consumerChannel;
      ThreadPoolExecutor executorService;
      try {
         executorService = new ThreadPoolExecutor(
               configuration.getMaxConsumers(),
               configuration.getMaxConsumers(),
               0,
               TimeUnit.MILLISECONDS,
               new LinkedBlockingQueue<Runnable>());
         Connection consumerConnection = factory.newConnection(executorService);
         allConnections.add(consumerConnection);
         consumerChannel = consumerConnection.createChannel();
      } catch (Exception e) {
         throw new JAsyncException("Error creating consumer connection", e);
      }

      var rabbitWorker = new RabbitWorker<>(
            consumerChannel,
            executorService,
            worker,
            itemClass,
            serializationStrategy,
            publishChannelPool);
      workers.put(worker.getClass(), rabbitWorker);
   }

   @Override
   public <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException {
      var worker = (RabbitWorker<T>) workers.get(workerType);
      if (worker == null) {
         throw new JAsyncException(workerType + " is not registered as a worker!");
      }
      worker.queueWorkItem(workItem);
   }

   @Override
   public void close() {
      logger.info("Closing down environment " + uuid);
      for (Connection connection : allConnections) {
         connection.abort();
      }
   }
}
