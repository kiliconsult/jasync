package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Just a thing that relates a publisher and a consumer to each other
 * @param <T>
 */
class RabbitWorker<T> {
   
   private static Logger logger = LoggerFactory.getLogger(RabbitWorker.class);
   private final RabbitConsumer<T> consumer;
   private final RabbitPublisher<T> publisher;

   public RabbitWorker(
         RabbitConsumer<T> consumer,
         RabbitPublisher<T> publisher) throws JAsyncException {
      this.consumer = consumer;
      this.publisher = publisher;
   }

   public void queueWorkItem(T workItem) throws JAsyncException {
      publisher.publishDirect(workItem, getQueueName());
   }

   public String getQueueName() {
      return consumer.getQueueName();
   }

   public void start() throws JAsyncException {
      this.consumer.start();
   }
}
