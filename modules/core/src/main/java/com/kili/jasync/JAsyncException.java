package com.kili.jasync;

public class JAsyncException extends Exception {

   public JAsyncException(String message) {
      super(message);
   }

   public JAsyncException(String message, Throwable cause) {
      super(message, cause);
   }

}
