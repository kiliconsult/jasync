package com.kili.jasync.transaction;

/**
 * No op transaction factory
 */
public class NoTransactionFactory implements Transaction {
   @Override
   public void commit() throws Exception {

   }

   @Override
   public void rollback() throws Exception {

   }

   @Override
   public void close() throws Exception {

   }
}
