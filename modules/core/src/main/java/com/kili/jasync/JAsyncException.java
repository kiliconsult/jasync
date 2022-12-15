package com.kili.jasync;

public class JAsyncException extends Exception {

   public JAsyncException() {
   }

   public JAsyncException(String message) {
      super(message);
   }

   public JAsyncException(String message, Throwable cause) {
      super(message, cause);
   }

   public JAsyncException(Throwable cause) {
      super(cause);
   }

   public JAsyncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
