package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.serialization.SerializationStrategy;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

class RabbitPublisher {

   private static final Logger logger = LoggerFactory.getLogger(RabbitPublisher.class);
   private static final int PUBLISH_TIMEOUT = 5_000;
   private final ObjectPool<Channel> publishChannelPool;
   private final SerializationStrategy serializationStrategy;

   private final Set<String> declaredQueues = new HashSet<>();
   private final Set<String> declaredExchanges = new HashSet<>();

   public RabbitPublisher(ObjectPool<Channel> publishChannelPool, SerializationStrategy serializationStrategy) {
      this.publishChannelPool = publishChannelPool;
      this.serializationStrategy = serializationStrategy;
   }

   <T> void publishDirect(T workItem, String exchange, String queueName) throws JAsyncException {
      publish(workItem, queueName, exchange, BuiltinExchangeType.DIRECT);
   }

   <T> void publishRouted(T message, String exchange, String route) throws JAsyncException {
      publish(message, route, exchange, BuiltinExchangeType.TOPIC);
   }

   private <T> void publish(T message, String route, String exchange, BuiltinExchangeType exchangeType) throws JAsyncException {
      byte[] serialized;
      try {
         serialized = serializationStrategy.serialize(message);
      } catch (Exception e) {
         throw new JAsyncException("Error serializing work item", e);
      }

      Channel channel = null;
      try {
         channel = publishChannelPool.borrowObject();
         if (!declaredExchanges.contains(exchange)) {
            channel.exchangeDeclare(exchange, exchangeType, true);
            declaredExchanges.add(exchange);
         }
         channel.basicPublish(exchange, route, null, serialized);
         channel.waitForConfirmsOrDie(PUBLISH_TIMEOUT);
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
