package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.*;
import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
class RabbitMQAsyncEnvironmentTest extends AbstractWorkerContractTest {

   @Container
   public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq"))
         .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("RabbitMQContainer")));


   @Override
   public AsyncEnvironment createEnvironment() throws JAsyncException {
      RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration.Builder(
            rabbitMQContainer.getAdminUsername(),
            rabbitMQContainer.getAdminPassword(),
            rabbitMQContainer.getHost())
            .setPort(rabbitMQContainer.getAmqpPort())
            .build();
      return RabbitMQAsyncEnvironment.create(rabbitMQConfiguration);
   }

   @Test
   public void testCustomSerializer() throws JAsyncException, InterruptedException {
      // TODO
   }

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