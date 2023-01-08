package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.fail.FailedItem;
import com.kili.jasync.serialization.SerializationException;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

class RabbitConsumer<T> extends DefaultConsumer {

   private static final Logger logger = LoggerFactory.getLogger(RabbitConsumer.class);
   private ThreadPoolExecutor executor;
   private Consumer<T> consumer;
   private Class<T> itemClass;
   private SerializationStrategy serializationStrategy;
   private QueueNameStrategy queueNameStrategy;

   public RabbitConsumer(
         Channel channel,
         ThreadPoolExecutor executor,
         Consumer<T> consumer,
         Class<T> itemClass,
         SerializationStrategy serializationStrategy,
         QueueNameStrategy queueNameStrategy) {
      super(channel);
      this.executor = executor;
      this.consumer = consumer;
      this.itemClass = itemClass;
      this.serializationStrategy = serializationStrategy;
      this.queueNameStrategy = queueNameStrategy;
   }

   @Override
   public void handleDelivery(
         String consumerTag,
         Envelope envelope,
         AMQP.BasicProperties properties,
         byte[] body) throws IOException {
      logger.debug("Handling message consumer=" + consumer);

      T deserialized = null;
      try {
         deserialized = serializationStrategy.deserialize(itemClass, body);
      } catch (Exception e) {
         throw new SerializationException("Error deserializing message", e);
      }

      try {
         consumer.consume(deserialized);
      } catch (Exception e) {
         try {
            FailedItem<T> failedItem = new FailedItem<>(deserialized, e);
            consumer.handleUncaughtException(failedItem);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      } finally {
         getChannel().basicAck(envelope.getDeliveryTag(), false);
      }
   }

   public String getQueueName() {
      return queueNameStrategy.getQueueName(consumer, itemClass);
   }

   public void start() throws JAsyncException {
      String queueName = getQueueName();
      try {
         Channel channel = getChannel();
         channel.queueDeclare(queueName, true, false, false, Map.of());
         channel.basicQos(executor.getCorePoolSize());
         channel.basicConsume(queueName, false, this);
      } catch (IOException e) {
         throw new JAsyncException("Error initializing rabbit consumer", e);
      }
   }

}
