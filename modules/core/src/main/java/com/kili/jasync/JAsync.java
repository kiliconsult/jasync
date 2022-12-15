package com.kili.jasync;

import com.kili.jasync.environment.AsyncEnvironment;

import java.util.HashMap;
import java.util.Map;

public class JAsync {

   private static Map<String, AsyncEnvironment> asyncEnvironments = new HashMap<>();

   public static void registerEnvironment(String name, AsyncEnvironment asyncEnvironment) {
      asyncEnvironments.put(name, asyncEnvironment);
   }

   public static AsyncEnvironment getEnvironment(String name) {
      return asyncEnvironments.get(name);
   }
}
