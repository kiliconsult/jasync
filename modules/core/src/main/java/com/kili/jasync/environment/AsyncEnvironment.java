package com.kili.jasync.environment;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.MessageHandlerConfiguration;
import com.kili.jasync.consumer.WorkerConfiguration;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.Consumer;

/**
 * An environment providing asynchronous functionality. Environments are meant to be thread safe, and each unique
 * environment should not need to be initialized more than once.
 */
public interface AsyncEnvironment extends AutoCloseable {

   /**
    * Initialize a worker in this environment. Workers are for handling background tasks
    * @param worker the worker
    * @param itemClass the work item type
    * @param configuration configuration of the worker
    */
   <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, WorkerConfiguration configuration) throws JAsyncException;

   /**
    * Initialize a message handler in this environment.
    * @param consumer the consumer of the message
    * @param itemClass the work item type
    * @param configuration configuration of the handler
    * @param <T> the type of messages that are handled
    */
   <T> void initializeMessageHandler(Consumer<T> consumer, Class<T> itemClass, MessageHandlerConfiguration configuration) throws JAsyncException;

   /**
    * Add work item to be handled later by a worker
    * @param workerType the type of the worker that should handle the work item
    * @param workItem the work item
    * @param <T> type of the work item
    * @throws JAsyncException if a worker of the given type is not found
    */
   <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException;

   /**
    * Send a message with a route. The message will be directed to the bound message handlers by the implemented backend.
    * @param route the route
    * @param message the message
    * @param <T> type of the message
    */
   <T> void sendRoutedMessage(String route, T message) throws JAsyncException;

   /**
    * Get information like queue size for the queue for a given consumer handling a type of messages
    * @param consumerType the type of the consumer
    * @param messageType the type of messages handled by the consumer
    */
   <T> QueueInfo getQueueInfo(Class<? extends Consumer<T>> consumerType, Class<T> messageType) throws JAsyncException;

   /**
    * Closes the environment and all resources used by the environment.
    */
   void close();
}
