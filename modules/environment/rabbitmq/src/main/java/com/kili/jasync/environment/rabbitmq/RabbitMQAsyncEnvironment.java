package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.*;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.environment.Exchange;
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
   private ConsumerRegistry<RabbitConsumer<?>> consumerRegistry = new ConsumerRegistry<>();
   private final RabbitPublisher publisher;
   private final SerializationStrategy serializationStrategy;
   private GenericObjectPool<Channel> queueInfoChannelPool;
   private Set<Connection> allConnections = new HashSet<>();
   private ConnectionFactory connectionFactory;
   private UUID uuid;

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
      publishChannelPool.setMaxIdle(rabbitMQConfiguration.getPublisherPoolSize() > 1 ? rabbitMQConfiguration.getPublisherPoolSize() / 2 : 1);
      publishChannelPool.setMaxTotal(rabbitMQConfiguration.getPublisherPoolSize());

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
         GenericObjectPool<Channel> queueInfoChannelPool) {
      this.uuid = uuid;
      this.connectionFactory = connectionFactory;
      this.allConnections = connectionsSet;
      this.serializationStrategy = serializationStrategy;
      this.queueInfoChannelPool = queueInfoChannelPool;
      this.publisher = new RabbitPublisher(publishChannelPool, serializationStrategy);
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


      Class<? extends Consumer<T>> aClass = (Class<? extends Consumer<T>>) worker.getClass();
      consumerRegistry.registerConsumer(aClass, itemClass, rabbitConsumer);

      String exchangeName = "";
      Exchange exchange = itemClass.getDeclaredAnnotation(Exchange.class);
      if (exchange != null && exchange.value() != null && exchange.value().trim().length() != 0) {
         exchangeName = exchange.value();
      }

      rabbitConsumer.startDirectConsumer(exchangeName, rabbitConsumer.getQueueName());
   }

   private <T> RabbitConsumer<T> createRabbitConsumer(
         Consumer<T> consumer,
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
               new NamedThreadFactory(consumer.getClass().getSimpleName()));
         var consumerConnection = connectionFactory.newConnection(executorService);
         allConnections.add(consumerConnection);
         var consumerChannel = consumerConnection.createChannel();
         rabbitConsumer = new RabbitConsumer<>(
               consumerChannel,
               executorService,
               consumer,
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
         Consumer<T> consumer,
         Class<T> itemClass,
         MessageHandlerConfiguration configuration) throws JAsyncException {
      logger.info("Initializing rabbit message handler {}", consumer);

      Exchange exchange = itemClass.getDeclaredAnnotation(Exchange.class);
      if (exchange == null || exchange.value() == null || exchange.value().trim().length() == 0) {
         throw new JAsyncException("Found no exchange on the message type. Expected @Exchange annotation with name of the exchange!");
      }
      String exchangeName = exchange.value();

      RabbitConsumer<T> rabbitConsumer = createRabbitConsumer(
            consumer,
            itemClass,
            configuration.getNumberOfConsumers(),
            new QueueNameStrategy() {
               @Override
               public <T> String getQueueName(Consumer<T> consumer, Class<T> messageType) {
                  return "message." + consumer.getClass().getName() + "." + itemClass.getName();
               }
            });

      Class<? extends Consumer<T>> aClass = (Class<? extends Consumer<T>>) consumer.getClass();
      consumerRegistry.registerConsumer(aClass, itemClass, rabbitConsumer);

      rabbitConsumer.startRoutedConsumer(exchangeName, configuration.getRoutes());
   }

   @Override
   public <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException {
      String exchangeName = "";
      Exchange exchange = workItem.getClass().getDeclaredAnnotation(Exchange.class);
      if (exchange != null && exchange.value() != null && exchange.value().trim().length() != 0) {
         exchangeName = exchange.value();
      }

      Class<T> aClass = (Class<T>) workItem.getClass();
      var worker = (RabbitConsumer<T>) consumerRegistry.getConsumer(workerType, aClass);
      if (worker == null) {
         throw new JAsyncException(workerType + " is not registered as a worker!");
      }

      publisher.publishDirect(workItem, exchangeName, worker.getQueueName());
   }

   @Override
   public <T> void sendRoutedMessage(String route, T message) throws JAsyncException {
      Exchange exchange = message.getClass().getDeclaredAnnotation(Exchange.class);
      if (exchange == null || exchange.value() == null || exchange.value().trim().length() == 0) {
         throw new JAsyncException("Found no exchange on the message. Expected @Exchange annotation with name of the exchange!");
      }

      publisher.publishRouted(message, exchange.value(), route);
   }

   @Override
   public <T> QueueInfo getQueueInfo(Class<? extends Consumer<T>> consumerType, Class<T> messageType) throws JAsyncException {
      var consumer = (RabbitConsumer<?>) consumerRegistry.getConsumer(consumerType, messageType);
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
      consumerRegistry = new ConsumerRegistry<>();
   }
}
