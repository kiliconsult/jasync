package com.kili.jasync.environment;

import com.kili.jasync.transaction.NoTransactionFactory;
import com.kili.jasync.transaction.TransactionFactory;

public class ConsumerConfiguration {
   private TransactionFactory transactionFactory;
   private int maxWorkers;

   private ConsumerConfiguration() {
   }

   public TransactionFactory getTransactionFactory() {
      return transactionFactory;
   }

   public int getMaxWorkers() {
      return maxWorkers;
   }

   public static class Builder {

      public ConsumerConfiguration build() {
         ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration();
         consumerConfiguration.maxWorkers = 1;
         consumerConfiguration.transactionFactory = NoTransactionFactory::new;
         return consumerConfiguration;
      }
   }
}
