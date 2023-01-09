package com.kili.jasync.environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks which exchange the message uses
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Exchange {

   /**
    * The name of the exchange to send this message through
    * @return the name of the exchange
    */
   String value();
}
