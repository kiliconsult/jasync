package com.kili.jasync;

import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Tests that a persisted worker has the expected functionality. Environments can provide additional functionality
 * that should be tested by the environment itself.
 */
public abstract class AbstractPersistedWorkerContractTest extends AbstractWorkerContractTest {

   @Test
   public void testWorkerPicksUpPreQueuedMessages() throws JAsyncException, InterruptedException {
      TestConsumer slowWorker = new TestConsumer(10);
      int messagesCount = 100;

      try (AsyncEnvironment firstEnvironment = createEnvironment()) {
         firstEnvironment.initializeWorker(
               slowWorker,
               TestMessage.class,
               new ConsumerConfiguration.Builder().build());
         for (int i = 0; i < messagesCount; i++) {
            firstEnvironment.addWorkItem(slowWorker.getClass(), new TestMessage("Message " + i));
         }
         waitForQueueSizeLEQ(slowWorker, firstEnvironment, 75);
      }

      try (AsyncEnvironment secondEnvironment = createEnvironment()) {
         secondEnvironment.initializeWorker(
               slowWorker,
               TestMessage.class,
               new ConsumerConfiguration.Builder().build());
         TestHelper.wait(messagesCount, slowWorker::getCount, Duration.ofSeconds(10));
         waitForQueueSizeLEQ(slowWorker, secondEnvironment, 0);
      }
   }

   private static void waitForQueueSizeLEQ(
         TestConsumer slowWorker,
         AsyncEnvironment firstEnvironment,
         int expectedQueueSize) throws InterruptedException {
      TestHelper.wait(() -> {
         try {
            return firstEnvironment.getQueueInfo(slowWorker.getClass()).queueSize() <= expectedQueueSize;
         } catch (JAsyncException e) {
            throw new RuntimeException(e);
         }
      }, Duration.ofSeconds(10));
   }
}
