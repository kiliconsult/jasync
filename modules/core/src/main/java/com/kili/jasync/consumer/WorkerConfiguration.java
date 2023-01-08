package com.kili.jasync.consumer;


public class WorkerConfiguration {
   private int numberOfConsumers;

   public WorkerConfiguration(int numberOfConsumers) {
      this.numberOfConsumers = numberOfConsumers;
   }

   public int getNumberOfConsumers() {
      return numberOfConsumers;
   }

   public static class Builder {

      private int numberOfConsumers = 1;

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
       * Build a configuration
       * @return a new config
       */
      public WorkerConfiguration build() {
         if (numberOfConsumers <= 0) {
            throw new IllegalArgumentException("Number of consumers should be a positive number");
         }
         return new WorkerConfiguration(numberOfConsumers);
      }
   }
}
