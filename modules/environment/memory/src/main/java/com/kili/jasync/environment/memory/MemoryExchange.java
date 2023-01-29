package com.kili.jasync.environment.memory;

import com.kili.jasync.JAsyncException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

class MemoryExchange {

   private final String name;
   private final HashMap<String, List<MemoryConsumerQueue<?>>> routedConsumers = new HashMap<>();
   private final RouteToRegex routeToRegex = new RouteToRegex();

   public MemoryExchange(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void addRoute(String route, MemoryConsumerQueue<?> consumer) {
      List<MemoryConsumerQueue<?>> consumers = routedConsumers.getOrDefault(route, new ArrayList<>(1));
      consumers.add(consumer);
      routedConsumers.put(route, consumers);
   }

   public <T> void sendRoutedMessage(String route, T message) throws JAsyncException {
      for (String knownRoutes : routedConsumers.keySet()) {
         Pattern pattern = routeToRegex.convertToRegex(knownRoutes);
         if (pattern != null && pattern.matcher(route).find()) {
            List<MemoryConsumerQueue<?>> memoryConsumerQueues = routedConsumers.get(knownRoutes);
            for (MemoryConsumerQueue<?> memoryConsumerQueue : memoryConsumerQueues) {
               try {
                  MemoryConsumerQueue<T> queue = (MemoryConsumerQueue<T>) memoryConsumerQueue;
                  queue.queueWorkItem(message);
               } catch (ClassCastException e) {
                  throw new JAsyncException(memoryConsumerQueue + " bound on " + route + " can not handle messages of type " + message.getClass());
               }
            }
         }
      }
   }


}
