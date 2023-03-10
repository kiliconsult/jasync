package com.kili.jasync;

import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.consumer.WorkerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

/**
 * Tests that a worker has the expected functionality. Environments can provide additional functionality
 * that should be tested by the environment itself.
 */
public abstract class AbstractContractTest {

   public abstract AsyncEnvironment createEnvironment() throws JAsyncException;

   @Test
   public void testEnvironmentStartAndClose() throws JAsyncException {
      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {
         // intentionally empty
      }
   }

   @Test
   public void testWorkerHandlesAllWorkItems() throws JAsyncException, InterruptedException {
      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {
         TestConsumer worker = new TestConsumer() {};
         asyncEnvironment.initializeWorker(
               worker,
               TestMessage.class,
               new WorkerConfiguration.Builder().setNumberOfConsumers(10).build());

         int messagesCount = 100;
         for (int i = 0; i < messagesCount; i++) {
            asyncEnvironment.addWorkItem(worker.getClass(), new TestMessage("Message " + i));
         }
         TestHelper.wait(messagesCount, worker::getCount, Duration.ofSeconds(30));
      }
   }

   @Test
   public void testAddWorkItemFailsOnClosedEnvironment() throws JAsyncException, InterruptedException {
      TestConsumer worker = new TestConsumer() {};

      AsyncEnvironment asyncEnvironment = createEnvironment();
      try (asyncEnvironment) {
         asyncEnvironment.initializeWorker(
               worker,
               TestMessage.class,
               new WorkerConfiguration.Builder().setNumberOfConsumers(10).build());
      }

      try {
         asyncEnvironment.addWorkItem(worker.getClass(), new TestMessage("Message should never be added"));
         Assertions.fail("Should never reach this statement");
      } catch (JAsyncException ignore) {
      } catch (Exception e) {
         Assertions.fail("Should throw a " + JAsyncException.class.getName());
      }

      Assertions.assertEquals(0, worker.getCount());
   }

   @Test
   public void testNumberOfWorkerConsumers() throws JAsyncException, InterruptedException {
      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {

         int numberOfConsumers = 4;

         TestConsumer worker = new TestConsumer() {};
         asyncEnvironment.initializeWorker(
               worker,
               TestMessage.class,
               new WorkerConfiguration.Builder().setNumberOfConsumers(numberOfConsumers).build());

         int messagesCount = 1000;
         for (int i = 0; i < messagesCount; i++) {
            asyncEnvironment.addWorkItem(worker.getClass(), new TestMessage("Message " + i));
         }

         TestHelper.wait(numberOfConsumers, () -> worker.getThreadNames().size(), Duration.ofSeconds(30));
      }
   }

   @Test
   public void testMessageHandlerHandlesAllMessages() throws JAsyncException, InterruptedException {
      try (AsyncEnvironment asyncEnvironment = createEnvironment()) {
         TestConsumer messageConsumerA = new TestConsumer() {};
         asyncEnvironment.initializeMessageHandler(
               messageConsumerA,
               TestMessage.class,
               new MessageHandlerConfiguration(5, Collections.singleton("message.a")));

         TestConsumer messageConsumerB = new TestConsumer() {};
         asyncEnvironment.initializeMessageHandler(
               messageConsumerB,
               TestMessage.class,
               new MessageHandlerConfiguration(5, Collections.singleton("message.b")));

         TestConsumer messageConsumerHash = new TestConsumer() {};
         asyncEnvironment.initializeMessageHandler(
               messageConsumerHash,
               TestMessage.class,
               new MessageHandlerConfiguration(5, Collections.singleton("message.#")));

         for (int i = 0; i < 100; i++) {
            asyncEnvironment.sendRoutedMessage("message.a", new TestMessage("Message A " + i));
         }
         for (int i = 0; i < 50; i++) {
            asyncEnvironment.sendRoutedMessage("message.b", new TestMessage("Message B " + i));
         }

         TestHelper.wait(100, messageConsumerA::getCount, Duration.ofSeconds(10));
         TestHelper.wait(50, messageConsumerB::getCount, Duration.ofSeconds(10));
         TestHelper.wait(150, messageConsumerHash::getCount, Duration.ofSeconds(10));
      }
   }
}
