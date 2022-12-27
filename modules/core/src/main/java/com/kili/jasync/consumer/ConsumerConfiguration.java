package com.kili.jasync.consumer;


public class ConsumerConfiguration {
   private int maxConsumers;

   private ConsumerConfiguration() {
   }

   public int getMaxConsumers() {
      return maxConsumers;
   }

   public static class Builder {

      private int maxConsumers = 1;

      public Builder setMaxConsumers(int maxConsumers) {
         this.maxConsumers = maxConsumers;
         return this;
      }

      public ConsumerConfiguration build() {
         ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration();
         consumerConfiguration.maxConsumers = maxConsumers;
         return consumerConfiguration;
      }
   }
}
