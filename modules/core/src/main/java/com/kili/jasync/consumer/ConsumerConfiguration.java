package com.kili.jasync.consumer;


public class ConsumerConfiguration {
   private int numberOfConsumers;

   private ConsumerConfiguration() {
   }

   public int getNumberOfConsumers() {
      return numberOfConsumers;
   }

   public static class Builder {

      private int numberOfConsumers = 1;

      public Builder setNumberOfConsumers(int maxConsumers) {
         this.numberOfConsumers = maxConsumers;
         return this;
      }

      public ConsumerConfiguration build() {
         ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration();
         consumerConfiguration.numberOfConsumers = numberOfConsumers;
         return consumerConfiguration;
      }
   }
}
