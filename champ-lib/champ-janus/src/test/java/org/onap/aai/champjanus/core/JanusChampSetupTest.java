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
package org.onap.aai.champjanus.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.aai.champjanus.graph.impl.JanusChampGraphImpl;

import java.util.HashMap;
import java.util.Map;

public class JanusChampSetupTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void JanusSetupBadBackendTest() {
    exception.expect(RuntimeException.class);
    JanusChampGraphImpl graph = new JanusChampGraphImpl.Builder("testGraph")
          .property("storage.backend", "bad-backend")
          .build();
  }

  @Test
  public void JanusSetupBerkleyBackendTest() {
    exception.expect(RuntimeException.class);
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put("storage.backend", "berkleyje");
    JanusChampGraphImpl graph = new JanusChampGraphImpl.Builder("testGraph")
          .properties(propertiesMap)
          .build();
  }

  @Test
  public void JanusSetupBadPropertyTest() {
      exception.expect(RuntimeException.class);
      JanusChampGraphImpl graph = new JanusChampGraphImpl.Builder("testGraph")
          .property("storage.backend", "in-memory")
          .property("storage.cassandra.keyspace", "anything")
          .build();
  }

  @Test
  public void JanusSetupBadPropertiesTest() {
    exception.expect(RuntimeException.class);
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put("storage.cassandra.keyspace", "anything");

    JanusChampGraphImpl graph = new JanusChampGraphImpl.Builder("testGraph")
        .property("storage.backend", "in-memory")
        .properties(propertiesMap)
        .build();
  }
}
