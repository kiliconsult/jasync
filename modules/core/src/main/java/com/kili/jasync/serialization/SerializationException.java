package com.kili.jasync.serialization;

/**
 * Thrown on serialization problems
 */
public class SerializationException extends RuntimeException {

   public SerializationException(String message, Throwable cause) {
      super(message, cause);
   }
}
