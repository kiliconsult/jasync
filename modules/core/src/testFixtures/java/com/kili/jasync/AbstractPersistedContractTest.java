package com.kili.jasync;

import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.consumer.WorkerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Tests that a persisted worker has the expected functionality. Environments can provide additional functionality
 * that should be tested by the environment itself.
 */
public abstract class AbstractPersistedContractTest extends AbstractContractTest {

   @Test
   public void testWorkerPicksUpPreQueuedMessages() throws JAsyncException, InterruptedException {
      TestConsumer slowWorker = new TestConsumer(10);
      int messagesCount = 100;

      try (AsyncEnvironment firstEnvironment = createEnvironment()) {
         firstEnvironment.initializeWorker(
               slowWorker,
               TestMessage.class,
               new WorkerConfiguration.Builder().build());
         for (int i = 0; i < messagesCount; i++) {
            firstEnvironment.addWorkItem(slowWorker.getClass(), new TestMessage("Message " + i));
         }
         waitForQueueSizeLEQ(slowWorker, firstEnvironment, 75);
      }

      try (AsyncEnvironment secondEnvironment = createEnvironment()) {
         secondEnvironment.initializeWorker(
               slowWorker,
               TestMessage.class,
               new WorkerConfiguration.Builder().build());
         TestHelper.wait(messagesCount, slowWorker::getCount, Duration.ofSeconds(10));
         waitForQueueSizeLEQ(slowWorker, secondEnvironment, 0);
      }
   }

   @Test
   public void testMessageHandlerPicksUpPreQueuedMessages() throws JAsyncException, InterruptedException {
      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {
         // Exchange is the event type or maybe the package name of the event
         var messagesCount = 100;
         for (var i = 0; i < messagesCount; i++) {
            // Should this fail if the exchange does not exist, or just create the exchange?
            // Could also end up in a dead letter handling. Would probably be better.
            asyncEnvironment.sendRoutedMessage("route", new TestMessage("Message " + i));
         }
      }

      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {
         var consumer = new TestConsumer() {};

         // Will create a queue with name like [consumer_type].[event_type]
         // The queue is bound to the event exchange with the route topic
         asyncEnvironment.initializeMessageHandler(
               consumer,
               TestMessage.class,
               new MessageHandlerConfiguration.Builder()
                     .setNumberOfConsumers(10)
                     .listenToRoute("route")
                     .build());
      }
   }
}
