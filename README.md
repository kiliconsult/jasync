[Build workflow]!(https://github.com/kiliconsult/jasync/actions/workflows/gradle.yml/badge.svg)

# JAsync

Working with async tool like RabbitMQ or Kafka introduces lots of new ways to fail as a developer. 
This project aims at reducing the cognitive load for developers working with asynchronous tasks 
or communication.

## Getting started

Register into a memory environment:

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