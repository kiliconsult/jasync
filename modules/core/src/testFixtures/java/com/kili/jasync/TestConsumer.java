package com.kili.jasync;

import com.kili.jasync.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestConsumer implements Consumer<TestMessage> {

   private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

   private final Set<TestMessage> uniqueMessages = Collections.synchronizedSet(new HashSet<>());
   private final Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
   private long sleepTime;

   public TestConsumer() {
   }

   public TestConsumer(long sleepTime) {
      this.sleepTime = sleepTime;
   }

   public int getCount() {
      return uniqueMessages.size();
   }

   @Override
   public void consume(TestMessage workItem) throws InterruptedException {
      if (sleepTime > 0) {
         Thread.sleep(sleepTime);
      }
      uniqueMessages.add(workItem);
      logger.info(workItem.message());
      String name = Thread.currentThread().getName();
      threadNames.add(name);
   }

   public Set<String> getThreadNames() {
      return threadNames;
   }
}
