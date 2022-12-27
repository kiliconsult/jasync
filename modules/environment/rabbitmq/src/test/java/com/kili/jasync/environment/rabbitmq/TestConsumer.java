package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class TestConsumer implements Consumer<TestMessage> {

   private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

   private AtomicInteger count = new AtomicInteger();

   public int getCount() {
      return count.get();
   }

   @Override
   public void consume(TestMessage workItem) {
      count.incrementAndGet();
      logger.info(workItem.message());
   }
}
