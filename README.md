![Build status](https://github.com/kiliconsult/jasync/actions/workflows/gradle.yml/badge.svg)

![Logo](https://i.imgur.com/QyFxWBc.png)

Working with async tools like RabbitMQ or Kafka introduces yet another steep learning curve for developers. 
Using messaging for communication between services or using background tasks should be simple.

This project aims to reduce the cognitive load and make development streamlined and stress-free.

## Supported environments
Environments are set up as *at least once*.

- RabbitMQ
- Memory

## Getting started with workers

Workers are great for tasks that you want to offload to the background such that it does not interfere with the main thread, 
which can keep serving other requests.

Workers can help you control resource usage by throttling how many workers/consumers your application can handle 
at a time.


![Workers](./docs/worker.png?raw=true)

### Example code

We will create a simple work item and a worker that simply logs a message asynchronously.

The work items are simple records/beans:

```java
public record WorkItem(String message) { }
```

And the consumer is an implementation of the Consumer interface:
```java
import com.kili.jasync.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleConsumer implements Consumer<WorkItem> {

   private static final Logger logger = LoggerFactory.getLogger(ExampleConsumer.class);

   @Override
   public void consume(WorkItem workItem) {
      logger.info(workItem.message());
   }
}
```

Register the worker on an environment. Here we choose the memory environment, but this could be
any environment:
```java
void initialize() throws JAsyncException {
   AsyncEnvironment asyncEnvironment = new MemoryAsyncEnvironment();
   asyncEnvironment.initializeWorker(new ExampleConsumer(), WorkItem.class, new ConsumerConfiguration.Builder().build());
   JAsyncRegistry.registerEnvironment("memory", asyncEnvironment);
}
```

Adding items to the worker:
```java
void publish(String message) throws JAsyncException {
   AsyncEnvironment memory = JAsyncRegistry.getEnvironment("memory");
   memory.addWorkItem(ExampleConsumer.class, new WorkItem(message));
}
```


## Using RabbitMQ

It is easy to use change the environment to a RabbitMQ cluster. Just change the AsyncEnvironment in the 
[Getting Started](#getting-started) to use the RabbitMQAsyncEnvironment.

```java
public AsyncEnvironment createEnvironment() throws JAsyncException {
   RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration.Builder(
      rabbitMQContainer.getAdminUsername(),
      rabbitMQContainer.getAdminPassword(),
      rabbitMQContainer.getHost())
   .setPort(rabbitMQContainer.getAmqpPort())
   .build();
   return RabbitMQAsyncEnvironment.create(rabbitMQConfiguration);
}
```

RabbitMQ is set up with sane defaults to ensure a healthe environment and that messages are handled "At least once".