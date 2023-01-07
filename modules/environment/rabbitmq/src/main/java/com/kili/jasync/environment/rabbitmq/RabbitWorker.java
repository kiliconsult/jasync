package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.fail.FailedItem;
import com.kili.jasync.serialization.SerializationException;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.*;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadPoolExecutor;

class RabbitWorker<T> extends DefaultConsumer {
   
   private static Logger logger = LoggerFactory.getLogger(RabbitWorker.class);
   private ThreadPoolExecutor consumerThreadPool;
   private Consumer<T> worker;
   private Class<T> itemClass;
   private SerializationStrategy serializationStrategy;
   private ObjectPool<Channel> publishChannelPool;

   public RabbitWorker(Channel consumerChannel, ThreadPoolExecutor consumerThreadPool, Consumer<T> worker, Class<T> itemClass, SerializationStrategy serializationStrategy, ObjectPool<Channel> publishChannelPool) throws JAsyncException {
      super(consumerChannel);
      this.consumerThreadPool = consumerThreadPool;
      this.worker = worker;
      this.itemClass = itemClass;
      this.serializationStrategy = serializationStrategy;
      this.publishChannelPool = publishChannelPool;

      try {
         String queueName = getQueueName();
         consumerChannel.queueDeclare(queueName, true, false, false, Map.of());
         consumerChannel.basicQos(consumerThreadPool.getCorePoolSize());
         consumerChannel.basicConsume(getQueueName(), false,this);
      } catch (IOException e) {
         throw new JAsyncException("Error initializing rabbit consumer", e);
      }
   }

   public void queueWorkItem(T workItem) throws JAsyncException {
      byte[] serialized;
      try {
         serialized = serializationStrategy.serialize(workItem);
      } catch (Exception e) {
         throw new JAsyncException("Error serializing work item", e);
      }

      Channel channel = null;

      try {
         channel = publishChannelPool.borrowObject();
         String queueName = getQueueName();
         channel.queueDeclare(queueName, true, false, false, Map.of());
         channel.basicPublish("", queueName, null, serialized);
      } catch (NoSuchElementException e) {
         throw new JAsyncException("Publisher is too busy", e);
      } catch (Exception e) {
         try {
            if (channel != null) {
               publishChannelPool.invalidateObject(channel);
            }
         } catch (Exception ex) {
            logger.error("Error publishing", e);
            throw new JAsyncException("Error publishing and error invalidating publisher channel", ex);
         }
         throw new JAsyncException("Error publishing", e);
      } finally {
         try {
            if (channel != null) {
               publishChannelPool.returnObject(channel);
            }
         } catch (Exception e) {
            logger.error("Publisher channel is in an unknown state", e);
         }
      }
   }

   @Override
   public void handleDelivery(
         String consumerTag,
         Envelope envelope,
         AMQP.BasicProperties properties,
         byte[] body) throws IOException {
      T deserialized = null;
      try {
         deserialized = serializationStrategy.deserialize(itemClass, body);
      } catch (Exception e) {
         throw new SerializationException("Error deserializing message", e);
      }

      try {
         worker.consume(deserialized);
      } catch (Exception e) {
         try {
            FailedItem<T> failedItem = new FailedItem<>(deserialized, e);
            worker.handleUncaughtException(failedItem);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      } finally {
         getChannel().basicAck(envelope.getDeliveryTag(), false);
      }
   }

   private String getQueueName() {
      return "worker." + Util.shortenClassName(worker.getClass());
   }

   public int getQueueSize() throws JAsyncException {
      try {
         AMQP.Queue.DeclareOk declareOk = getChannel().queueDeclarePassive(getQueueName());
         return declareOk.getMessageCount();
      } catch (IOException e) {
         throw new JAsyncException("Could not fetch queue size for " + this.getClass().getName());
      }
   }
}
