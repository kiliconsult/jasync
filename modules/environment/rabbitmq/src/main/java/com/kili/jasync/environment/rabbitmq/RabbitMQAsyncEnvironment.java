package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.NamedThreadFactory;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Environment that uses a RabbitMQ backend
 */
public class RabbitMQAsyncEnvironment implements AsyncEnvironment {

   private static final Logger logger = LoggerFactory.getLogger(RabbitMQAsyncEnvironment.class);
   private final Map<Class<? extends Consumer>, RabbitWorker<?>> workers = new HashMap<>();
   private final SerializationStrategy serializationStrategy;
   private GenericObjectPool<Channel> publishChannelPool;
   private Set<Connection> allConnections = new HashSet<>();
   private UUID uuid;
   private ConnectionFactory connectionFactory;

   public static RabbitMQAsyncEnvironment create(RabbitMQConfiguration rabbitMQConfiguration) throws JAsyncException {
      Connection globalPublisherConnection;
      var connectionFactory = new ConnectionFactory();
      var connectionsSet = new HashSet<Connection>();
      try {
         connectionFactory.setHost(rabbitMQConfiguration.getHostName());
         connectionFactory.setPort(rabbitMQConfiguration.getPort());
         connectionFactory.setUsername(rabbitMQConfiguration.getUserName());
         connectionFactory.setPassword(rabbitMQConfiguration.getPassword());
         connectionFactory.setVirtualHost(rabbitMQConfiguration.getVhost());
         globalPublisherConnection = connectionFactory.newConnection();
         connectionsSet.add(globalPublisherConnection);
      } catch (Exception e) {
         throw new JAsyncException("Unable to create rabbit connection", e);
      }

      var publishChannelPool = new GenericObjectPool<>(new RabbitChannelFactory(globalPublisherConnection));
      publishChannelPool.setMinIdle(1);
      publishChannelPool.setMaxIdle(3);
      publishChannelPool.setMaxTotal(5);

      var serializationStrategy = rabbitMQConfiguration.getSerializationStrategy();

      var uuid = UUID.randomUUID();
      var environment = new RabbitMQAsyncEnvironment(uuid, connectionFactory, connectionsSet, serializationStrategy, publishChannelPool);
      logger.info("initialized environment " + uuid);
      return environment;
   }

   private RabbitMQAsyncEnvironment(
         UUID uuid,
         ConnectionFactory connectionFactory,
         HashSet<Connection> connectionsSet,
         SerializationStrategy serializationStrategy,
         GenericObjectPool<Channel> publishChannelPool) throws JAsyncException {
      this.uuid = uuid;
      this.connectionFactory = connectionFactory;
      this.allConnections = connectionsSet;
      this.serializationStrategy = serializationStrategy;
      this.publishChannelPool = publishChannelPool;
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
               new LinkedBlockingQueue<>(),
               new NamedThreadFactory(worker.getClass().getSimpleName()));
         Connection consumerConnection = connectionFactory.newConnection(executorService);
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
