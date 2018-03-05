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
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.*;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationship.ReservedPropertyKeys;
import org.onap.aai.champcore.model.ChampRelationship.ReservedTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class ChampRelationshipTest extends BaseChampAPITest {

  @Test
  public void runInMemoryTest() {
    runTest("IN_MEMORY");
  }

  public void runTest(String apiType) {
    final String graphName = ChampRelationshipTest.class.getSimpleName();

    final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
    ChampRelationshipTest.testChampRelationshipCrud(api.getGraph(graphName));
    api.shutdown();
  }

  public static void testChampRelationshipCrud(ChampGraph graph) {
    final ChampObject source = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property1", "value1")
        .build();

    final ChampObject target = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .build();

    try {
      ChampTransaction tx = ChampRelationshipTest.getTransaction();
      
      final ChampObject storedSource = graph.storeObject(source, Optional.of(tx));
      final ChampObject storedTarget = graph.storeObject(target, Optional.of(tx));

      final ChampRelationship relationship = new ChampRelationship.Builder(storedSource, storedTarget, "relationship")
          .property("property-1", "value-1")
          .property("property-2", 3)
          .build();

      final ChampRelationship storedRelationship = graph.storeRelationship(relationship, Optional.of(tx));
      final Optional<ChampRelationship> retrievedRelationship = graph.retrieveRelationship(storedRelationship.getKey().get(), Optional.of(tx));

      if (!retrievedRelationship.isPresent()) {
        throw new AssertionError("Failed to retrieve stored relationship " + storedRelationship);
      }
      if (!storedRelationship.equals(retrievedRelationship.get())) {
        throw new AssertionError("Retrieved relationship does not equal stored object");
      }

      assertTrue(retrievedRelationship.get().getProperty("property-1").get().equals("value-1"));
      assertTrue(retrievedRelationship.get().getProperty("property-2").get().equals(3));

      if (!graph.retrieveRelationships(storedRelationship.getSource(), Optional.of(tx)).collect(Collectors.toList()).contains(storedRelationship)) {
        throw new AssertionError("Failed to retrieve relationships for source object");
      }

      final ChampRelationship updatedRelationship = ChampRelationship.create()
          .from(retrievedRelationship.get())
          .withKey(retrievedRelationship.get().getKey().get())
          .withProperty("property-2", 4)
          .build();

      final ChampRelationship storedUpdRel = graph.storeRelationship(updatedRelationship, Optional.of(tx));
      final Optional<ChampRelationship> retrievedUpdRel = graph.retrieveRelationship(storedUpdRel.getKey().get(), Optional.of(tx));

      assertTrue(retrievedUpdRel.isPresent());
      assertTrue(retrievedUpdRel.get().equals(storedUpdRel));
      assertTrue(retrievedUpdRel.get().getProperty("property-1").get().equals("value-1"));
      assertTrue(retrievedUpdRel.get().getProperty("property-2").get().equals(4));


      // validate the replaceRelationship method
      final ChampRelationship replacedRelationship = new ChampRelationship.Builder(storedSource, storedTarget, "relationship")
          .key(retrievedRelationship.get().getKey().get())
          .property("property-2", 4)
          .build();

      final ChampRelationship replacedRel = graph.replaceRelationship(replacedRelationship, Optional.of(tx));
      final Optional<ChampRelationship> retrievedReplacedRel = graph
          .retrieveRelationship(replacedRel.getKey().get(), Optional.of(tx));

      assertTrue(replacedRel.getProperties().size() == 1);
      assertTrue(replacedRel.getProperty("property-2").get().equals(4));

      assertTrue(retrievedReplacedRel.get().getProperties().size() == 1);
      assertTrue(retrievedReplacedRel.get().getProperty("property-2").get().equals(4));

      if (!retrievedReplacedRel.isPresent()) {
        throw new AssertionError("Failed to retrieve stored relationship " + replacedRel);
      }
      if (!replacedRel.equals(retrievedReplacedRel.get())) {
        throw new AssertionError("Retrieved relationship does not equal stored object");
      }


      graph.deleteRelationship(retrievedRelationship.get(), Optional.of(tx));

      if (graph.retrieveRelationship(relationship.getKey(), Optional.of(tx)).isPresent()) {
        throw new AssertionError("Relationship not successfully deleted");
      }

      try {
        graph.deleteRelationship(retrievedRelationship.get(), Optional.of(tx));
        throw new AssertionError("Failed to throw exception for missing relationship");
      } catch (ChampRelationshipNotExistsException e) {
        //Expected
      }
      
      Map<String, Object> queryParams = new HashMap<>();
      queryParams.put(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString(), "blah");
      graph.queryRelationships(queryParams);
      
      queryParams = new HashMap<>();
      queryParams.put(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString(), "blah");
      graph.queryRelationships(queryParams);

      assertTrue(graph.queryRelationships(Collections.emptyMap(), Optional.of(tx)).count() == 0);
      assertTrue(graph.queryObjects(Collections.emptyMap(), Optional.of(tx)).count() == 2);
    } catch (ChampSchemaViolationException e) {
      throw new AssertionError("Schema mismatch while storing object", e);
    } catch (ChampMarshallingException e) {
      throw new AssertionError("Marshalling exception while storing object", e);
    } catch (ChampUnmarshallingException e) {
      throw new AssertionError("Unmarshalling exception while retrieving relationship", e);
    } catch (ChampRelationshipNotExistsException e) {
      throw new AssertionError("Attempted to delete non-existent relationship", e);
    } catch (ChampObjectNotExistsException e) {
      throw new AssertionError("Object does not exist after storing it", e);
    } catch (ChampTransactionException e) {
      throw new AssertionError("Transaction failure", e);
    }

    ChampTransaction tx = ChampRelationshipTest.getTransaction();
    try {
      graph.retrieveRelationships(ChampObject.create().ofType("").withoutKey().build(), Optional.of(tx));
      throw new AssertionError("Failed to handle missing object while retrieving relationships");
    } catch (ChampUnmarshallingException | ChampTransactionException e) {
      throw new AssertionError(e);
    } catch (ChampObjectNotExistsException e) {
      //Expected
    }
    //Negative test cases for replace relationship

    try {
      graph.replaceRelationship(new ChampRelationship.Builder(ChampObject.create()
          .ofType("foo")
          .withoutKey()
          .build(), ChampObject.create()
          .ofType("foo")
          .withoutKey()
          .build(), "relationship")
          .key("1234")
          .property("property-2", 4)
          .build(),
          Optional.of(tx));
    } catch (ChampUnmarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampMarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampSchemaViolationException e) {
      throw new AssertionError(e);
    } catch (ChampRelationshipNotExistsException e) {
      throw new AssertionError(e);
    } catch (IllegalArgumentException e) {
      //expected
    } catch (ChampTransactionException e) {
      throw new AssertionError(e);
    }

    try {
      graph.replaceRelationship(new ChampRelationship.Builder(ChampObject.create()
          .ofType("foo")
          .withKey("123")
          .build(), ChampObject.create()
          .ofType("foo")
          .withKey("456")
          .build(), "relationship")
          .property("property-2", 4)
          .build(), 
          Optional.of(tx));
    } catch (ChampUnmarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampMarshallingException e) {
      throw new AssertionError(e);
    } catch (ChampSchemaViolationException e) {
      throw new AssertionError(e);
    } catch (ChampRelationshipNotExistsException e) {
      //expected
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (ChampTransactionException e) {
      throw new AssertionError(e);
    }


  }

  @Test
  public void testFluentRelationshipCreation() {
    final Object value1 = new Object();
    final String value2 = "value2";
    final float value3 = 0.0f;

    final ChampRelationship champRelationship = ChampRelationship.create()
        .ofType("foo")
        .withoutKey()
        .withSource()
        .ofType("bar")
        .withoutKey()
        .build()
        .withTarget()
        .ofType("baz")
        .withKey(1)
        .build()
        .withProperty("key1", value1)
        .withProperty("key2", value2)
        .withProperty("key3", value3)
        .build();

    ChampTransaction tx = ChampRelationshipTest.getTransaction();
    assertTrue(champRelationship.getKey().equals(Optional.empty()));
    assertTrue(champRelationship.getType().equals("foo"));
    assertTrue(champRelationship.getProperty("key1").get() instanceof Object);
    assertTrue(champRelationship.getProperty("key1").get().equals(value1));
    assertTrue(champRelationship.getProperty("key2").get() instanceof String);
    assertTrue(champRelationship.getProperty("key2").get().equals(value2));
    assertTrue(champRelationship.getProperty("key3").get() instanceof Float);
    assertTrue(champRelationship.getProperty("key3").get().equals(value3));
  }

  @Test
  public void testChampRelationshipEnums() {
    for (ReservedPropertyKeys key : ChampRelationship.ReservedPropertyKeys.values()) {
      assertTrue(ChampRelationship.ReservedPropertyKeys.valueOf(key.name()) == key);
    }

    for (ReservedTypes type : ChampRelationship.ReservedTypes.values()) {
      assertTrue(ChampRelationship.ReservedTypes.valueOf(type.name()) == type);
    }
  }
  
  public static ChampTransaction getTransaction() {
    return new TestTransaction();
  }
  
  public static class TestTransaction extends ChampTransaction {

    public TestTransaction() {
      
    }
    @Override
    public void commit() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void rollback() {
      // TODO Auto-generated method stub
      
    }
    @Override
    public String id() {
      // TODO Auto-generated method stub
      return null;
    }
    
  }

}
