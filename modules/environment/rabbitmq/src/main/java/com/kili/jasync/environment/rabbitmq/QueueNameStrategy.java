package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.consumer.Consumer;

/**
 * A strategy for naming rabbit queues
 */
public interface QueueNameStrategy {

   <T> String getQueueName(Consumer<T> consumer, Class<T> messageType);
}
