package dk.kili.jasync;

import com.kili.jasync.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestConsumer implements Consumer<TestMessage> {

   private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

   private final AtomicInteger count = new AtomicInteger();
   private final Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());

   public int getCount() {
      return count.get();
   }

   @Override
   public void consume(TestMessage workItem) {
      count.incrementAndGet();
      logger.info(workItem.message());
      String name = Thread.currentThread().getName();
      threadNames.add(name);
   }

   public Set<String> getThreadNames() {
      return threadNames;
   }
}
