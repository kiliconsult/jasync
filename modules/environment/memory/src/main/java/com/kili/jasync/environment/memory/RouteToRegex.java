package com.kili.jasync.environment.memory;

import java.util.HashMap;
import java.util.regex.Pattern;

class RouteToRegex {

   HashMap<String, Pattern> patternCache = new HashMap<>();

   Pattern convertToRegex(String route) {
      if (patternCache.containsKey(route)) {
         return patternCache.get(route);
      }
      String pattern = route.replaceAll("\\.", "\\\\.")
            .replaceAll("\\*", ".*\\.")
            .replaceAll("#", ".*");
      Pattern compiled = Pattern.compile("^" + pattern + "$");
      patternCache.put(route, compiled);
      return compiled;
   }

}
