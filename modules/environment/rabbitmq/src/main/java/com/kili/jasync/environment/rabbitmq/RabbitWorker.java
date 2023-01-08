package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.fail.FailedItem;
import com.kili.jasync.serialization.SerializationException;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.*;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

class RabbitWorker<T> {
   
   private static Logger logger = LoggerFactory.getLogger(RabbitWorker.class);
   private Channel consumerChannel;
   private final ThreadPoolExecutor consumerThreadPool;
   private final RabbitConsumer<T> consumer;
   private final RabbitPublisher<T> publisher;

   public RabbitWorker(
         Channel consumerChannel,
         ThreadPoolExecutor consumerThreadPool,
         Consumer<T> consumer,
         Class<T> itemClass,
         SerializationStrategy serializationStrategy,
         GenericObjectPool<Channel> publishChannelPool) throws JAsyncException {
      this.consumerChannel = consumerChannel;
      this.consumerThreadPool = consumerThreadPool;
      this.consumer = new RabbitConsumer<>(consumerChannel, consumer, itemClass, serializationStrategy);
      this.publisher = new RabbitPublisher<>("", publishChannelPool, serializationStrategy);

      try {
         String queueName = getQueueName();
         consumerChannel.queueDeclare(queueName, true, false, false, Map.of());
         consumerChannel.basicQos(consumerThreadPool.getCorePoolSize());
         consumerChannel.basicConsume(getQueueName(), false, this.consumer);
      } catch (IOException e) {
         throw new JAsyncException("Error initializing rabbit consumer", e);
      }
   }

   public void queueWorkItem(T workItem) throws JAsyncException {
      publisher.publishDirect(workItem, getQueueName());
   }

   private String getQueueName() {
      return "worker." + Util.shortenClassName(consumer.getClass());
   }

   public int getQueueSize() throws JAsyncException {
      try {
         AMQP.Queue.DeclareOk declareOk = consumerChannel.queueDeclarePassive(getQueueName());
         return declareOk.getMessageCount();
      } catch (IOException e) {
         throw new JAsyncException("Could not fetch queue size for " + this.getClass().getName());
      }
   }
}
