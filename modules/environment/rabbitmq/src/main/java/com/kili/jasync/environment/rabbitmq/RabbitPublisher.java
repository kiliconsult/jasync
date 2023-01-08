package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NoSuchElementException;

class RabbitPublisher<T> {

   private static final Logger logger = LoggerFactory.getLogger(RabbitPublisher.class);
   private String exchangeName;
   private ObjectPool<Channel> publishChannelPool;
   private SerializationStrategy serializationStrategy;

   public RabbitPublisher(String exchangeName, ObjectPool<Channel> publishChannelPool, SerializationStrategy serializationStrategy) {
      this.exchangeName = exchangeName;
      this.publishChannelPool = publishChannelPool;
      this.serializationStrategy = serializationStrategy;
   }

   void publishDirect(T workItem, String queueName) throws JAsyncException {
      byte[] serialized;
      try {
         serialized = serializationStrategy.serialize(workItem);
      } catch (Exception e) {
         throw new JAsyncException("Error serializing work item", e);
      }

      Channel channel = null;

      try {
         channel = publishChannelPool.borrowObject();
         channel.queueDeclare(queueName, true, false, false, Map.of());
         channel.basicPublish(exchangeName, queueName, null, serialized);
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
}
