package com.kili.jasync.consumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Extends the default thread factory that normally names threads with
 * <pre>
 * pool-4-thread-1
 * </pre>
 * and replaces the word pool with
 * <pre>
 * [PREFIX]-[POOL_NUMBER]-[THREAD_NUMBER]
 * </pre>.
 */
public class NamedThreadFactory implements ThreadFactory {

   private String prefix;

   public NamedThreadFactory(String prefix) {
      this.prefix = prefix;
   }

   public Thread newThread(Runnable r) {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      String newName = thread.getName().replace("pool", prefix).replace("-thread-", "-");
      thread.setName(newName);
      return thread;
   }
}
