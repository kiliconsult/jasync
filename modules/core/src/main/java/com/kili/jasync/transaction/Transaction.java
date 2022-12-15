package com.kili.jasync.transaction;

public interface Transaction extends AutoCloseable {

   void commit() throws Exception;

   void rollback() throws Exception;
}
