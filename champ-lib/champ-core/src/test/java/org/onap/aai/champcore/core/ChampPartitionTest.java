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
package org.onap.aai.champcore.core;

import org.junit.Test;
import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.exceptions.*;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class ChampPartitionTest extends org.onap.aai.champcore.core.BaseChampAPITest {

  @Test
  public void runInMemoryTests() {
    runTests("IN_MEMORY");
  }

  public void runTests(String apiType) {
    final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
    final String graphName = ChampPartitionTest.class.getSimpleName();

    ChampPartitionTest.testChampPartitionCrud(api.getGraph(graphName));
    api.shutdown();
  }

  @Test
  public void testPartitionToString() throws Exception {
    final ChampObject foo = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .build();
    final ChampObject bar = ChampObject.create()
        .ofType("bar")
        .withoutKey()
        .build();
    final ChampRelationship baz = ChampRelationship.create()
        .ofType("baz")
        .withoutKey()
        .withSource()
        .from(foo)
        .build()
        .withTarget()
        .from(bar)
        .build()
        .build();

    final ChampPartition partition = ChampPartition.create()
        .withObject(foo)
        .withObject(bar)
        .withRelationship(baz)
        .build();

    assertTrue(partition.toString().equals("{objects: [{key: , type: bar, properties: {}},{key: , type: foo, properties: {}}], relationships: [{key: , type: baz, source: {key: , type: foo, properties: {}}, target: {key: , type: bar, properties: {}}, properties: {}}]}"));
    //"{objects: [{key: \"\", type: \"foo\", properties: {}}], relationships: []}"
    //"{objects: [{key: , type: foo, properties: {}}], relationships: []}"
    //throw new Exception(partition.toString());
  }

  @Test
  public void testHashCode() {

    final ChampObject foo = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .build();
    final ChampObject bar = ChampObject.create()
        .ofType("bar")
        .withoutKey()
        .build();
    final ChampRelationship baz = ChampRelationship.create()
        .ofType("baz")
        .withoutKey()
        .withSource()
        .from(foo)
        .build()
        .withTarget()
        .from(bar)
        .build()
        .build();

    final ChampPartition partition = ChampPartition.create()
        .withObject(foo)
        .withObject(bar)
        .withRelationship(baz)
        .build();

    assertTrue(partition.getChampObjects().contains(foo));
    assertTrue(partition.getChampObjects().contains(bar));
    assertTrue(partition.getChampRelationships().contains(baz));
  }

  @Test
  public void testBuilder() {
    final ChampObject foo = new ChampObject.Builder("foo").build();
    final ChampObject bar = new ChampObject.Builder("bar").build();
    final ChampRelationship uses = new ChampRelationship.Builder(foo, bar, "uses")
        .build();
    final ChampPartition a = new ChampPartition.Builder()
        .object(foo)
        .objects(Collections.singleton(bar))
        .relationship(uses)
        .relationships(Collections.singleton(uses))
        .build();
    assertTrue(a.getChampObjects().size() == 2);
    assertTrue(a.getChampObjects().contains(foo));
    assertTrue(a.getChampObjects().contains(bar));

    assertTrue(a.getChampRelationships().size() == 1);
    assertTrue(a.getChampRelationships().contains(uses));
  }

  public static void testChampPartitionCrud(ChampGraph graph) {

    final ChampObject foo = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("prop1", "value1")
        .build();
    final ChampObject bar = ChampObject.create()
        .ofType("bar")
        .withoutKey()
        .withProperty("prop2", "value2")
        .build();

    final ChampRelationship baz = ChampRelationship.create()
        .ofType("baz")
        .withoutKey()
        .withSource()
        .from(foo)
        .build()
        .withTarget()
        .from(bar)
        .build()
        .withProperty("prop3", "value3")
        .build();

    final ChampPartition partition = ChampPartition.create()
        .withObject(foo)
        .withObject(bar)
        .withRelationship(baz)
        .build();

    assertTrue(partition.getIncidentRelationships(foo).contains(baz));
    assertTrue(partition.getIncidentRelationships(bar).contains(baz));
    assertTrue(partition.getIncidentRelationshipsByType(foo).get("baz").contains(baz));

    try {
      final ChampPartition storedPartition = graph.storePartition(partition);

      ChampPartitionTest.retrievePartitionElements(graph, storedPartition, true);

      graph.deletePartition(storedPartition);

      ChampPartitionTest.retrievePartitionElements(graph, storedPartition, false);

    } catch (ChampMarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampObjectNotExistsException e) {
      throw new AssertionError(e);
    } catch (ChampSchemaViolationException e) {
      throw new AssertionError(e);
    } catch (ChampRelationshipNotExistsException e) {
      throw new AssertionError(e);
    } catch (ChampTransactionException e) {
      throw new AssertionError(e);
    }
  }

  private static void retrievePartitionElements(ChampGraph graph, ChampPartition partition, boolean expectFound) {
    for (ChampObject object : partition.getChampObjects()) {
      try {
        final Optional<ChampObject> retrievedObject = graph.retrieveObject(object.getKey().get());

        if (!expectFound && retrievedObject.isPresent()) {
          throw new AssertionError("Expected object to not be found, but it was found");
        }
        if (expectFound && !retrievedObject.isPresent()) {
          throw new AssertionError("Expected object to be found, but it was not found");
        }
      } catch (ChampUnmarshallingException | ChampTransactionException e) {
        throw new AssertionError(e);
      }
    }

    for (ChampRelationship relationship : partition.getChampRelationships()) {
      try {
        final Optional<ChampRelationship> retrievedRelationship = graph.retrieveRelationship(relationship.getKey().get());

        if (!expectFound && retrievedRelationship.isPresent()) {
          throw new AssertionError("Expected relationship to not be found, but it was found");
        }
        if (expectFound && !retrievedRelationship.isPresent()) {
          throw new AssertionError("Expected relationship to be found, but it was not found");
        }
      } catch (ChampUnmarshallingException | ChampTransactionException e) {
        throw new AssertionError(e);
      }
    }
  }
}
