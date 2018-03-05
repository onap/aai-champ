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
import org.onap.aai.champcore.exceptions.*;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipIndex;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class ChampRelationshipIndexTest extends BaseChampAPITest {

  @Test
  public void runInMemoryTest() {
    runTest("IN_MEMORY");
  }

  public void runTest(String apiType) {
    final String graphName = ChampRelationshipIndexTest.class.getSimpleName();

    final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
    testChampRelationshipIndexCrud(api.getGraph(graphName));
    api.shutdown();
  }

  public void testChampRelationshipIndexCrud(ChampGraph graph) {

    final ChampField relationshipField = new ChampField.Builder("propertyName").build();
    final ChampRelationshipIndex relationshipIndex = new ChampRelationshipIndex.Builder("fooEdgeIndex", "foo", relationshipField).build();

    //Test on an empty graph
    testChampRelationshipIndexStorage(graph, relationshipIndex);
    testChampRelationshipIndexDelete(graph, relationshipIndex);

    //Test with existing data in graph
    try {
      graph.storeRelationship(ChampRelationship.create()
          .ofType("uses")
          .withoutKey()
          .withSource()
          .ofType("foo")
          .withoutKey()
          .build()
          .withTarget()
          .ofType("bar")
          .withoutKey()
          .build()
          .build()
        , Optional.empty());
      testChampRelationshipIndexStorage(graph, relationshipIndex);
      testChampRelationshipIndexDelete(graph, relationshipIndex);
    } catch (ChampMarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampSchemaViolationException e) {
      throw new AssertionError(e);
    } catch (ChampObjectNotExistsException e) {
      throw new AssertionError(e);
    } catch (ChampRelationshipNotExistsException e) {
      throw new AssertionError(e);
    } catch (ChampUnmarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampTransactionException e) {
      throw new AssertionError(e);
    }
  }

  private void testChampRelationshipIndexDelete(ChampGraph graph, ChampRelationshipIndex relationshipIndex) {

    if (!graph.capabilities().canDeleteRelationshipIndices()) {
      try {
        graph.deleteRelationshipIndex("someindex");
        throw new AssertionError("Graph claims it doesn't support relationship index delete, but it failed to throw UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
        //Expected
      } catch (ChampIndexNotExistsException e) {
        throw new AssertionError("Graph claims it doesn't support relationship index delete, but it failed to throw UnsupportedOperationException");
      }
    } else {
      try {
        graph.deleteRelationshipIndex(relationshipIndex.getName());

        final Optional<ChampRelationshipIndex> retrieveRelationshipIndex = graph.retrieveRelationshipIndex(relationshipIndex.getName());

        if (retrieveRelationshipIndex.isPresent()) {
          throw new AssertionError("Retrieve relationship index after deleting it");
        }

        final Stream<ChampRelationshipIndex> relationshipIndices = graph.retrieveRelationshipIndices();
        final Collection<ChampRelationshipIndex> allRelationshipIndices = relationshipIndices.collect(Collectors.toList());

        if (allRelationshipIndices.contains(relationshipIndex)) {
          throw new AssertionError("Retrieve all relationship indices contains previously deleted index");
        }
        if (allRelationshipIndices.size() != 0) {
          throw new AssertionError("Wrong number of relationship indices returned by retrieve all indices");
        }
      } catch (ChampIndexNotExistsException e) {
        throw new AssertionError(e);
      }

      try {
        graph.deleteRelationshipIndex(relationshipIndex.getName());
        throw new AssertionError("Failed to throw exception on non-existent object index");
      } catch (ChampIndexNotExistsException e) {
        //Expected
      }
    }
  }

  private void testChampRelationshipIndexStorage(ChampGraph graph, ChampRelationshipIndex relationshipIndex) {

    graph.storeRelationshipIndex(relationshipIndex);
    graph.storeRelationshipIndex(relationshipIndex); //Test storing duplicate relationship index

    assertTrue(!graph.retrieveObjectIndex(relationshipIndex.getName()).isPresent()); //Make sure this wasn't stored as an object index

    final Optional<ChampRelationshipIndex> retrieveRelationshipIndex = graph.retrieveRelationshipIndex(relationshipIndex.getName());

    if (!retrieveRelationshipIndex.isPresent()) {
      throw new AssertionError("Failed to retrieve relationship index after storing it");
    }
    if (!relationshipIndex.equals(retrieveRelationshipIndex.get())) {
      throw new AssertionError("Non-equal relationship index returned from API after storing it");
    }

    final Stream<ChampRelationshipIndex> relationshipIndices = graph.retrieveRelationshipIndices();
    final Collection<ChampRelationshipIndex> allRelationshipIndices = relationshipIndices.collect(Collectors.toList());

    if (!allRelationshipIndices.contains(relationshipIndex)) {
      throw new AssertionError("Retrieve all relationship indices did not return previously stored relationship index");
    }
    if (allRelationshipIndices.size() != 1) {
      throw new AssertionError("Wrong number of relationship indices returned by retrieve all indices");
    }

    assertTrue(!graph.retrieveRelationshipIndex("nonExistentIndexName").isPresent());
  }

  @Test
  public void testFluentRelationshipIndexCreation() {
    final ChampRelationshipIndex relationshipIndex = ChampRelationshipIndex.create()
        .ofName("fooNameIndex")
        .onType("foo")
        .forField("name")
        .build();

    assertTrue(relationshipIndex.getName().equals("fooNameIndex"));
    assertTrue(relationshipIndex.getType().equals("foo"));
    assertTrue(relationshipIndex.getField().getName().equals("name"));
  }
}
