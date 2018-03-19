/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
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
 */
package org.onap.aai.champtitan.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.aai.champtitan.graph.impl.TitanChampGraphImpl;

import java.util.HashMap;
import java.util.Map;

public class TitanChampSetupTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void TitanSetupBadBackendTest() {
    exception.expect(RuntimeException.class);
    TitanChampGraphImpl graph = new TitanChampGraphImpl.Builder("testGraph")
          .property("storage.backend", "bad-backend")
          .build();
  }

  @Test
  public void TitanSetupBerkleyBackendTest() {
    exception.expect(RuntimeException.class);
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put("storage.backend", "berkleyje");
    TitanChampGraphImpl graph = new TitanChampGraphImpl.Builder("testGraph")
          .properties(propertiesMap)
          .build();
  }

  @Test
  public void TitanSetupBadPropertyTest() {
      exception.expect(RuntimeException.class);
      TitanChampGraphImpl graph = new TitanChampGraphImpl.Builder("testGraph")
          .property("storage.backend", "in-memory")
          .property("storage.cassandra.keyspace", "anything")
          .build();
  }

  @Test
  public void TitanSetupBadPropertiesTest() {
    exception.expect(RuntimeException.class);
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put("storage.cassandra.keyspace", "anything");

    TitanChampGraphImpl graph = new TitanChampGraphImpl.Builder("testGraph")
        .property("storage.backend", "in-memory")
        .properties(propertiesMap)
        .build();
  }
}
