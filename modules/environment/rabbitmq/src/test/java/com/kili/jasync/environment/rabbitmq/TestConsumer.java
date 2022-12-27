package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConsumer implements Consumer<TestMessage> {

   private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

   @Override
   public void consume(TestMessage workItem) {
      logger.info(workItem.message());
   }
}
