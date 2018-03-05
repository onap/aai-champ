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
package org.onap.aai.champcore.core;

import org.junit.Test;
import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.graph.impl.ChampAPIImpl;
import org.onap.aai.champcore.graph.impl.InMemoryChampGraphImpl;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

public class ChampAPITest {

  @Test
  public void testChampAPIMemoryInstantiation() {
    testChampAPIInstantiation(new ChampAPIImpl("IN_MEMORY"), "IN_MEMORY");
  }

  public void testChampAPIInstantiation(ChampAPI api, String expectedType) {
      assertTrue(api.getType() == expectedType);

      api.getGraph("foo");
      api.shutdown();

      try {
        api.getGraph("foo");
        throw new AssertionError("Able to call getGraph(String name) after shutdown()");
      } catch (IllegalStateException e) {
        //Expected
      }
  }

  @Test
  public void testChampMemoryGraphInstantiation() throws Exception
  {
    testChampGraphInstantiation(new InMemoryChampGraphImpl.Builder().build());
  }

  public void testChampGraphInstantiation(ChampGraph graph) throws Exception {

    graph.shutdown();

    try {
      graph.deleteObject(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.deleteObjectIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.deletePartition(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.deleteRelationship(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.deleteRelationshipIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.deleteSchema();
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.queryObjects(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.queryRelationships(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveObject(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveObjectIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveObjectIndices();
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveRelationship(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveRelationshipIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveRelationshipIndices();
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveRelationships(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.retrieveSchema();
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storeObject(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storeObjectIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storePartition(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storeRelationship(null, Optional.empty());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storeRelationshipIndex(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.storeSchema(null);
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.updateSchema(new ChampObjectConstraint.Builder("").build());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.updateSchema(new ChampRelationshipConstraint.Builder("").build());
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }

    try {
      graph.shutdown();
      throw new AssertionError("Able to call API method after shutdown was initiated");
    } catch (IllegalStateException e) {
      //Expected
    }
  }
}
