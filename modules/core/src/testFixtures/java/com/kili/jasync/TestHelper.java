package com.kili.jasync;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

public class TestHelper {

   public static void wait(Integer expected, Supplier<Integer> check, Duration duration) throws InterruptedException {
      LocalDateTime stopWaiting = LocalDateTime.now().plus(duration);
      Integer latestCheckValue = null;
      while (LocalDateTime.now().isBefore(stopWaiting)) {
         latestCheckValue = check.get();
         if (Objects.equals(latestCheckValue, expected)) {
            return;
         }
         Thread.sleep(10);
      }
      throw new RuntimeException("Check did not finish in time, expected " + expected + " but was " + latestCheckValue);
   }

   public static void wait(Supplier<Boolean> check, Duration duration) throws InterruptedException {
      LocalDateTime stopWaiting = LocalDateTime.now().plus(duration);
      while (LocalDateTime.now().isBefore(stopWaiting)) {
         if (check.get()) {
            return;
         }
         Thread.sleep(10);
      }
      throw new RuntimeException("Check did not finish in time");
   }
}
