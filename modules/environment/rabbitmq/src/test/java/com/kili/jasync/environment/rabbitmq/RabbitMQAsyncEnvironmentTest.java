package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.environment.AsyncEnvironment;
import dk.kili.jasync.AbstractWorkerContractTest;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
      RabbitMQAsyncEnvironment asyncEnvironment = RabbitMQAsyncEnvironment.create(rabbitMQConfiguration);
      return asyncEnvironment;
   }
}