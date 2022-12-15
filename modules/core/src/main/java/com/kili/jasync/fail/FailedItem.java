package com.kili.jasync.fail;

public class FailedItem<T> {
   private T workItem;
   private Exception cause;

   public FailedItem(T workItem, Exception cause) {
      this.workItem = workItem;
      this.cause = cause;
   }

   public int getRetryCount() {
      return 0;
   }

   public Throwable getCause() {
      return cause;
   }

   public void requeue() {
      throw new UnsupportedOperationException("Not yet!");
   }
}
