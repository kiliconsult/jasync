package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.ConsumerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RabbitMQAsyncEnvironmentTest {

   @Container
   public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq"))
         .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("RabbitMQContainer")));

   @Test
   public void testEnvironmentStartAndClose() throws JAsyncException {
      RabbitMQAsyncEnvironment asyncEnvironment = createNewEnvironment();
      asyncEnvironment.close();
   }

   @Test
   public void testWorkerHandlesAllMessages() throws JAsyncException, InterruptedException {
      RabbitMQAsyncEnvironment asyncEnvironment = createNewEnvironment();

      TestConsumer worker = new TestConsumer();
      asyncEnvironment.initializeWorker(
            worker,
            TestMessage.class,
            new ConsumerConfiguration.Builder().setMaxConsumers(10).build());

      int messagesCount = 100;
      for (int i = 0; i < messagesCount; i++) {
         asyncEnvironment.addWorkItem(TestConsumer.class, new TestMessage("Message " + i));
      }

      Thread.sleep(1000);
      Assertions.assertEquals(messagesCount, worker.getCount());

      asyncEnvironment.close();
   }

   @Test
   public void testAddWorkItemFailsOnClosedEnvironment() throws JAsyncException, InterruptedException {
      RabbitMQAsyncEnvironment asyncEnvironment = createNewEnvironment();

      TestConsumer worker = new TestConsumer();
      asyncEnvironment.initializeWorker(
            worker,
            TestMessage.class,
            new ConsumerConfiguration.Builder().setMaxConsumers(10).build());
      asyncEnvironment.close();

      try {
         asyncEnvironment.addWorkItem(TestConsumer.class, new TestMessage("Message should never be added"));
         Assertions.fail("Should never reach this statement");
      } catch (JAsyncException ignore) {
      } catch (Exception e) {
        Assertions.fail("Should throw a " + JAsyncException.class.getName());
      }

      Assertions.assertEquals(0, worker.getCount());
   }

   @Test
   public void testWorkerPicksUpPreQueuedMessages() throws JAsyncException, InterruptedException {
      // TODO
   }

   @Test
   public void testNumberOfWorkerConsumers() throws JAsyncException, InterruptedException {
      // TODO
   }

   @Test
   public void testCustomSerializer() throws JAsyncException, InterruptedException {
      // TODO
   }

   @NotNull
   private static RabbitMQAsyncEnvironment createNewEnvironment() throws JAsyncException {
      RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration.Builder(
            rabbitMQContainer.getAdminUsername(),
            rabbitMQContainer.getAdminPassword(),
            rabbitMQContainer.getHost())
            .setPort(rabbitMQContainer.getAmqpPort())
            .build();
      RabbitMQAsyncEnvironment asyncEnvironment = RabbitMQAsyncEnvironment.create(rabbitMQConfiguration);
      return asyncEnvironment;
   }
}