package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.*;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RabbitMQAsyncEnvironmentTest extends AbstractPersistedContractTest {

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


}