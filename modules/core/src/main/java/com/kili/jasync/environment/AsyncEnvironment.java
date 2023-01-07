package com.kili.jasync.environment;

import com.kili.jasync.QueueInfo;
import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.Consumer;

public interface AsyncEnvironment extends AutoCloseable {

   /**
    * Initialize a worker in this environment
    * @param worker the worker
    * @param itemClass the work item type
    * @param configuration configuration of the worker
    */
   <T> void initializeWorker(Consumer<T> worker, Class<T> itemClass, ConsumerConfiguration configuration) throws JAsyncException;

   /**
    * Add work item to a async queue
    * @param workerType the type of the worker that should handle the work item
    * @param workItem the work item
    * @param <T> type of the work item
    * @throws JAsyncException if a worker of the given type is not found
    */
   <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException;

   /**
    * Get information about the queue for a given consumer
    * @param consumerType the type of the consumer
    */
   QueueInfo getQueueInfo(Class<? extends Consumer<?>> consumerType) throws JAsyncException;

   void close();
}
