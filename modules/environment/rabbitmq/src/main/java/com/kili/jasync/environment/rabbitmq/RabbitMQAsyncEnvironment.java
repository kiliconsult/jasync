package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.consumer.NamedThreadFactory;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.consumer.WorkerConfiguration;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.AMQP;
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
   private GenericObjectPool<Channel> queueInfoChannelPool;
   private Set<Connection> allConnections = new HashSet<>();
   private UUID uuid;
   private ConnectionFactory connectionFactory;

   public static RabbitMQAsyncEnvironment create(RabbitMQConfiguration rabbitMQConfiguration) throws JAsyncException {
      Connection globalPublisherConnection;
      Connection globalQueueInfoConnection;
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
         globalQueueInfoConnection = connectionFactory.newConnection();
         connectionsSet.add(globalQueueInfoConnection);
      } catch (Exception e) {
         throw new JAsyncException("Unable to create rabbit connections", e);
      }

      var publishChannelPool = new GenericObjectPool<>(new RabbitChannelFactory(globalPublisherConnection));
      publishChannelPool.setMinIdle(1);
      publishChannelPool.setMaxIdle(3);
      publishChannelPool.setMaxTotal(5);

      var queueInfoChannelPool = new GenericObjectPool<>(new RabbitChannelFactory(globalQueueInfoConnection));
      publishChannelPool.setMinIdle(1);
      publishChannelPool.setMaxTotal(2);

      var serializationStrategy = rabbitMQConfiguration.getSerializationStrategy();

      var uuid = UUID.randomUUID();
      var environment = new RabbitMQAsyncEnvironment(uuid, connectionFactory, connectionsSet, serializationStrategy, publishChannelPool, queueInfoChannelPool);
      logger.info("initialized environment " + uuid);
      return environment;
   }

   private RabbitMQAsyncEnvironment(
         UUID uuid,
         ConnectionFactory connectionFactory,
         HashSet<Connection> connectionsSet,
         SerializationStrategy serializationStrategy,
         GenericObjectPool<Channel> publishChannelPool,
         GenericObjectPool<Channel> queueInfoChannelPool) throws JAsyncException {
      this.uuid = uuid;
      this.connectionFactory = connectionFactory;
      this.allConnections = connectionsSet;
      this.serializationStrategy = serializationStrategy;
      this.publishChannelPool = publishChannelPool;
      this.queueInfoChannelPool = queueInfoChannelPool;
   }

   @Override
   public <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, WorkerConfiguration configuration) throws JAsyncException {
      logger.info("Initializing rabbit worker {}", worker);

      RabbitConsumer<T> rabbitConsumer = createRabbitConsumer(
            worker,
            itemClass,
            configuration.getNumberOfConsumers(),
            new QueueNameStrategy() {
               @Override
               public <T> String getQueueName(Consumer<T> consumer, Class<T> messageType) {
                  return "worker." + consumer.getClass().getName();
               }
            });
      RabbitPublisher<T> rabbitPublisher = new RabbitPublisher<>("", publishChannelPool, serializationStrategy);

      var rabbitWorker = new RabbitWorker<>(
            rabbitConsumer,
            rabbitPublisher);
      workers.put(worker.getClass(), rabbitWorker);
      rabbitWorker.start();
   }

   private <T> RabbitConsumer<T> createRabbitConsumer(
         Consumer<T> worker,
         Class<T> itemClass,
         int numberOfConsumers,
         QueueNameStrategy queueNameStrategy) throws JAsyncException {
      RabbitConsumer<T> rabbitConsumer;
      try {
         var executorService = new ThreadPoolExecutor(
               numberOfConsumers,
               numberOfConsumers,
               0,
               TimeUnit.MILLISECONDS,
               new LinkedBlockingQueue<>(),
               new NamedThreadFactory(worker.getClass().getSimpleName()));
         var consumerConnection = connectionFactory.newConnection(executorService);
         allConnections.add(consumerConnection);
         var consumerChannel = consumerConnection.createChannel();
         rabbitConsumer = new RabbitConsumer<>(
               consumerChannel,
               executorService,
               worker,
               itemClass,
               serializationStrategy,
               queueNameStrategy);
      } catch (Exception e) {
         throw new JAsyncException("Error creating consumer connection", e);
      }
      return rabbitConsumer;
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
      var worker = (RabbitWorker<T>) workers.get(workerType);
      if (worker == null) {
         throw new JAsyncException(workerType + " is not registered as a worker!");
      }
      worker.queueWorkItem(workItem);
   }

   @Override
   public <T> void sendRoutedMessage(String route, T message) {
      throw new UnsupportedOperationException("Not yet supported!");
   }

   @Override
   public QueueInfo getQueueInfo(Class<? extends Consumer<?>> consumerType) throws JAsyncException {
      var consumer = (RabbitWorker<?>) workers.get(consumerType);
      if (consumer == null) {
         throw new JAsyncException(consumerType + " is not registered!");
      }

      Channel channel = null;
      try {
         channel = queueInfoChannelPool.borrowObject();
         AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(consumer.getQueueName());
         return new QueueInfo(declareOk.getMessageCount());
      } catch (Exception e) {
         throw new JAsyncException("Could not fetch queue info for " + consumerType);
      } finally {
         if (channel != null) {
            queueInfoChannelPool.returnObject(channel);
         }
      }
   }

   @Override
   public void close() {
      logger.info("Closing down environment " + uuid);
      for (Connection connection : allConnections) {
         connection.abort();
      }
   }
}
