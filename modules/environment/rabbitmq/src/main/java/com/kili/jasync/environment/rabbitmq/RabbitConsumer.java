package com.kili.jasync.environment.rabbitmq;

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

class RabbitConsumer<T> extends DefaultConsumer {

   private static final Logger logger = LoggerFactory.getLogger(RabbitConsumer.class);
   private Consumer<T> consumer;
   private Class<T> itemClass;
   private SerializationStrategy serializationStrategy;

   public RabbitConsumer(Channel channel, Consumer<T> consumer, Class<T> itemClass, SerializationStrategy serializationStrategy) {
      super(channel);
      this.consumer = consumer;
      this.itemClass = itemClass;
      this.serializationStrategy = serializationStrategy;
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

}
