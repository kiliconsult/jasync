package dk.kili.jasync;

import com.kili.jasync.JAsyncException;
import com.kili.jasync.consumer.ConsumerConfiguration;
import com.kili.jasync.environment.AsyncEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a worker has the expected functionality. Environments can provide additional functionality
 * that should be tested by the environment itself.
 */
public abstract class AbstractWorkerContractTest {

   public abstract AsyncEnvironment createEnvironment() throws JAsyncException;

   @Test
   public void testEnvironmentStartAndClose() throws JAsyncException {
      AsyncEnvironment asyncEnvironment = createEnvironment();
      asyncEnvironment.close();
   }

   @Test
   public void testWorkerHandlesAllMessages() throws JAsyncException, InterruptedException {
      AsyncEnvironment asyncEnvironment = createEnvironment();

      TestConsumer worker = new TestConsumer();
      asyncEnvironment.initializeWorker(
            worker,
            TestMessage.class,
            new ConsumerConfiguration.Builder().setMaxConsumers(10).build());

      int messagesCount = 100;
      for (int i = 0; i < messagesCount; i++) {
         asyncEnvironment.addWorkItem(TestConsumer.class, new TestMessage("Message " + i));
      }

      Thread.sleep(1000);
      assertEquals(messagesCount, worker.getCount());

      asyncEnvironment.close();
   }

   @Test
   public void testAddWorkItemFailsOnClosedEnvironment() throws JAsyncException, InterruptedException {
      AsyncEnvironment asyncEnvironment = createEnvironment();

      TestConsumer worker = new TestConsumer();
      asyncEnvironment.initializeWorker(
            worker,
            TestMessage.class,
            new ConsumerConfiguration.Builder().setMaxConsumers(10).build());
      asyncEnvironment.close();

      try {
         asyncEnvironment.addWorkItem(TestConsumer.class, new TestMessage("Message should never be added"));
         Assertions.fail("Should never reach this statement");
      } catch (JAsyncException ignore) {
      } catch (Exception e) {
         Assertions.fail("Should throw a " + JAsyncException.class.getName());
      }

      Assertions.assertEquals(0, worker.getCount());
   }

   @Test
   public void testWorkerPicksUpPreQueuedMessages() throws JAsyncException, InterruptedException {
      // TODO
   }

   @Test
   public void testNumberOfWorkerConsumers() throws JAsyncException, InterruptedException {
      // TODO
   }

   @Test
   public void testCustomSerializer() throws JAsyncException, InterruptedException {
      // TODO
   }
}
