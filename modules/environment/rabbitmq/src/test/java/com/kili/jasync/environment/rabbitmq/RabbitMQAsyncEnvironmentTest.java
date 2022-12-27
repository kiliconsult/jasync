package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.environment.ConsumerConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RabbitMQAsyncEnvironmentTest {

   @Container
   public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq"))
         .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("RabbitMQContainer")));

   @Test
   public void testEmptyRabbitEnvironment() throws JAsyncException {
      RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration.Builder(rabbitMQContainer.getAdminUsername(), rabbitMQContainer.getAdminPassword(), rabbitMQContainer.getHost())
            .setPort(rabbitMQContainer.getAmqpPort())
            .build();
      RabbitMQAsyncEnvironment asyncEnvironment = new RabbitMQAsyncEnvironment(rabbitMQConfiguration);
      asyncEnvironment.close();
   }

   @Test
   public void testRabbitWorker() throws JAsyncException, InterruptedException {
      RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration.Builder(rabbitMQContainer.getAdminUsername(), rabbitMQContainer.getAdminPassword(), rabbitMQContainer.getHost())
            .setPort(rabbitMQContainer.getAmqpPort())
            .build();
      RabbitMQAsyncEnvironment asyncEnvironment = new RabbitMQAsyncEnvironment(rabbitMQConfiguration);
      asyncEnvironment.initializeWorker(
            new TestConsumer(),
            TestMessage.class,
            new ConsumerConfiguration.Builder().setMaxConsumers(10).build());
      for (int i = 0; i < 100; i++) {
         asyncEnvironment.addWorkItem(TestConsumer.class, new TestMessage("Message " + i));
      }
      Thread.sleep(5000);
      asyncEnvironment.close();
   }

}