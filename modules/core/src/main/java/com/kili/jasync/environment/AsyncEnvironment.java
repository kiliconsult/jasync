package com.kili.jasync.environment;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.Consumer;

public interface AsyncEnvironment {

   /**
    * Initialize a worker in this environment
    * @param worker the worker
    * @param configuration configuration of the worker
    */
   void initializeWorker(Consumer<?> worker, ConsumerConfiguration configuration);

   /**
    * Add work item to a async queue
    * @param workerType the type of the worker that should handle the work item
    * @param workItem the work item
    * @param <T> type of the work item
    * @throws JAsyncException if a worker of the given type is not found
    */
   <T> void addWorkItem(Class<? extends Consumer<T>> workerType, T workItem) throws JAsyncException;
}
