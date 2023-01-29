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

<p align="center">
    <img src="https://i.imgur.com/7YmWhFs.png" alt="Workers">
</p>

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
any supported environment. Initialize JASYNC once in your application, for example on application startup.
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

## Getting started with messages

Messages are a form of asynchronous communication that are sent to an exchange, rather than a specific 
recipient. The exchange then routes the message to any consumers that have subscribed to it. 
This allows for decoupling between the sender and receiver, as the sender does not need to know who 
the message is being sent to. This is different from a worker, where the sender and receiver have 
a direct connection and specific knowledge of each other.

Example of a message being sent after a customer placed an order:

<p align="center">
    <img src="https://i.imgur.com/05edytb.png" alt="Messaging">
</p>

### Example code

We will create the order example from the picture above.

```java
@Exchange("orders")
public record CreatedOrderMessage(String orderId) { }
```

The `@Exchange` annotation is needed for messages. This tells which exchange you want to publish messages to 
or listen/consume messages from.

#### Publish side

If you were only to make the publishing side, then publishing this message would be

```java
void initialize() throws JAsyncException {
   AsyncEnvironment asyncEnvironment = createAsyncEnvironment(); // A Rabbit, Memory or other supported environment
   JAsyncRegistry.registerEnvironment("messaging", asyncEnvironment);
}
```

```java
void publish(String orderId) throws JAsyncException {
   AsyncEnvironment asyncEnvironment = JAsyncRegistry.getEnvironment("messaging");
   asyncEnvironment.sendRoutedMessage("order.created", new CreatedOrderMessage(orderId))
}
```

#### Consumer side

And consumers are again implementations of the Consumer interface. Here the invoice consumer

```java
import com.kili.jasync.consumer.Consumer;

public class InvoiceConsumer implements Consumer<CreatedOrderMessage> {

   @Override
   public void consume(CreatedOrderMessage message) {
      // Send invoice
   }
}
```

and the warehouse consumer

```java
import com.kili.jasync.consumer.Consumer;

public class WarehouseConsumer implements Consumer<CreatedOrderMessage> {

   @Override
   public void consume(CreatedOrderMessage message) {
      // Update stocks 
   }
}
```

The consumers would probably be split into different microservices, but could also live in a larger application together. 
For simplicity this is a more monolithic application that can handle both the invoice and the warehouse domains.

Again we initialize JASYNC once in the application on startup.

```java
void initialize() throws JAsyncException {
   AsyncEnvironment asyncEnvironment = createAsyncEnvironment(); // A Rabbit, Memory or other supported environment
   asyncEnvironment.initializeMessageHandler(
      new InvoiceConsumer(),
      CreatedOrderMessage.class,
      new MessageHandlerConfiguration.Builder()
         .addRoute("order.created")
         .build());
   asyncEnvironment.initializeMessageHandler(
      new WarehouseConsumer(),
      CreatedOrderMessage.class,
      new MessageHandlerConfiguration.Builder()
         .setNumberOfConsumers(5)
         .addRoute("order.created")
         .build());
   JAsyncRegistry.registerEnvironment("messaging", asyncEnvironment);
}
```

The consumers are now set up to listen to messages published to the `orders` exchange with the topic `order.created`. 
Thus they will start getting messages from the publisher side we created above.

You can also use the two AMQP keywords:

 - \# (hash) matches zero or more words, for example, “metrics.#” will match all routing keys that start with “metrics.”
 - \* (star) matches one word, for example, “metrics.*.cpu” will match all routing keys that start with “metrics.” and end in “.cpu”.

## Using RabbitMQ

It is easy to use change the environment to a RabbitMQ cluster. Just change the AsyncEnvironment in the 
[Getting Started](#getting-started-with-workers) to use the RabbitMQAsyncEnvironment.

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

RabbitMQ is set up with sane defaults to ensure a healthy environment and that messages are handled "At least once".