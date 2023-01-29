package com.kili.jasync.environment.memory;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RouteToRegexTest {

   @Test
   void testSimpleRoute() {
      String route = "route.that.is.simple";
      Pattern pattern = new RouteToRegex().convertToRegex(route);
      assertTrue(pattern.matcher(route).find());
      assertFalse(pattern.matcher("another.route").find());
   }

   @Test
   void testStarRoute() {
      Pattern pattern = new RouteToRegex().convertToRegex("route.with.*.are.smart");
      assertTrue(pattern.matcher("route.with.stars.are.smart").find());
      assertFalse(pattern.matcher("other.route.with.stars.are.smart").find());
   }

   @Test
   void testHashRoute() {
      Pattern pattern = new RouteToRegex().convertToRegex("route.with.#");
      assertTrue(pattern.matcher("route.with.hashes.are.smart").find());
      assertTrue(pattern.matcher("route.with.hashes").find());
      assertFalse(pattern.matcher("other.route.with.hashes").find());
   }
}