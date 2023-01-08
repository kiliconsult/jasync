package com.kili.jasync.environment.memory;

import com.kili.jasync.environment.AsyncEnvironment;
import com.kili.jasync.AbstractContractTest;

class MemoryAsyncEnvironmentTest extends AbstractContractTest {

   @Override
   public AsyncEnvironment createEnvironment() {
      return new MemoryAsyncEnvironment();
   }
}