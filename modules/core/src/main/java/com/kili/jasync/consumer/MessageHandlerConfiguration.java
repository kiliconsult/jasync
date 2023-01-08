package com.kili.jasync.consumer;


import java.util.HashSet;
import java.util.Set;

public class MessageHandlerConfiguration {
   private int numberOfConsumers;
   private Set<String> routes;

   public MessageHandlerConfiguration(int numberOfConsumers, Set<String> routes) {
      this.numberOfConsumers = numberOfConsumers;
      this.routes = routes;
   }

   public int getNumberOfConsumers() {
      return numberOfConsumers;
   }

   public Set<String> getRoutes() {
      return routes;
   }

   public static class Builder {

      private int numberOfConsumers = 1;
      private Set<String> routes = new HashSet<>();

      /**
       * Sets the number of consumers that should process messages from the queue. The default number is 1.
       *
       * @param numberOfConsumers the number of consumers
       * @return this builder
       */
      public Builder setNumberOfConsumers(int numberOfConsumers) {
         if (numberOfConsumers < 0) {
            throw new IllegalArgumentException("Number must be greater than 0");
         }
         this.numberOfConsumers = numberOfConsumers;
         return this;
      }

      /**
       * Listen to messages coming from the given route. It is possible to listen to multiple routes.
       * @param route the route to listen for
       * @return this builder
       */
      public Builder listenToRoute(String route) {
         routes.add(route);
         return this;
      }

      /**
       * Build a configuration
       * @return a new config
       */
      public MessageHandlerConfiguration build() {
         if (routes.isEmpty()) {
            throw new IllegalArgumentException("No routes configured");
         }
         if (numberOfConsumers <= 0) {
            throw new IllegalArgumentException("Number of consumers should be a positive number");
         }
         return new MessageHandlerConfiguration(numberOfConsumers, routes);
      }
   }
}
