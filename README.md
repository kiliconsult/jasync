![Build status](https://github.com/kiliconsult/jasync/actions/workflows/gradle.yml/badge.svg)

![Logo](https://imgur.com/a/jxKuDZt)

Working with async tool like RabbitMQ or Kafka introduces lots of new ways to fail as a developer. 
This project aims at reducing the cognitive load for developers working with asynchronous tasks 
or communication.

## Supported environments
- Memory
- RabbitMQ
- Kafka (Coming up)

## Getting started

We will create a simple work item and a worker that logs a message asynchronously.

The work items is a simple record:

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
