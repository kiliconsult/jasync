package com.kili.jasync;

import com.kili.jasync.consumer.Consumer;
import com.kili.jasync.fail.FailedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleConsumer implements Consumer<WorkItem> {

   private static final Logger logger = LoggerFactory.getLogger(ExampleConsumer.class);

   @Override
   public void consume(WorkItem workItem) {
      logger.info(workItem.message());
   }

   @Override
   public void handleUncaughtException(FailedItem<WorkItem> failedItem) {
      if (failedItem.getRetryCount() > 2) {
         logger.error("Went totally wrong!", failedItem.getCause());
      } else {
         failedItem.requeue();
      }
   }
}
