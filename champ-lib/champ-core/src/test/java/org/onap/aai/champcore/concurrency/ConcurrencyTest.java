/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.champcore.concurrency;

import org.junit.Test;
import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.core.ChampObjectTest;
import org.onap.aai.champcore.core.ChampRelationshipTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrencyTest.class);

  @Test
  public void runInMemoryConcurrentTest() {
    runConcurrentTest("IN_MEMORY");
  }

  public void runConcurrentTest(String apiType) {
    final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
    runConcurrencyTest(api);
    api.shutdown();
  }

  private void runConcurrencyTest(ChampAPI api) {
    final int numThreads = 10;
    final ExecutorService es = Executors.newFixedThreadPool(numThreads);

    for (int i = 0; i < numThreads * 2; i++) {
      es.submit(new Runnable() {
        @Override
        public void run() {
          final ChampGraph graph = api.getGraph(ConcurrencyTest.class.getSimpleName());
          ChampObjectTest.testChampObjectCrud(graph);
          ChampRelationshipTest.testChampRelationshipCrud(graph);
        }
      });
    }

    try {
      es.shutdown();
      es.awaitTermination(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for concurrency test to finish", e);
      return;
    }
  }
}
