package com.kili.jasync.environment.memory;

import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.AbstractWorkerContractTest;

class MemoryAsyncEnvironmentTest extends AbstractWorkerContractTest {

   @Override
   public AsyncEnvironment createEnvironment() {
      return new MemoryAsyncEnvironment();
   }
}