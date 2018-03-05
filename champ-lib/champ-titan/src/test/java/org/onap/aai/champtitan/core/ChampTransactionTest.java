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
package org.onap.aai.champtitan.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champtitan.graph.impl.TitanChampGraphImpl;

public class ChampTransactionTest {

  public TitanChampGraphImpl graph = null;
  public CountDownLatch latch = new CountDownLatch(2);
  public ChampObject[] storedVertices = new ChampObject[2];

  
  @Before
  public void setup() {
    graph = new TitanChampGraphImpl.Builder("TransactionTestGraph")
      .property("storage.backend", "inmemory")
      .build();
  }
  
  
  /**
   * This test validates that multiple CRUD operations can be performed against vertices within
   * the same transactional context and that, once the transaction is committed, the final 
   * state of the vertices reflects the sum of all of the operations performed within the
   * transaction.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws ChampUnmarshallingException
   * @throws ChampTransactionException 
   */
  @Test
  public void champObjectSingleTxTest() throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampUnmarshallingException, ChampTransactionException {

    ChampObject v1 = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property1", "value1")
        .withProperty("property2", "value2")
        .build();
    
    ChampObject v2 = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property3", "value3")
        .withProperty("property4", "value4")
        .build();
    
    ChampObject v3 = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property5", "value5")
        .withProperty("property6", "value6")
        .build();
    
    ChampObject v4 = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property7", "value7")
        .withProperty("property8", "value8")
        .build();
    
    
    // Open a transaction with the graph data store.
    ChampTransaction tx = graph.openTransaction();
    
      // Create all of our vertices.
      ChampObject storedV1 = graph.storeObject(v1, Optional.of(tx));
      ChampObject storedV2 = graph.storeObject(v2, Optional.of(tx));
      ChampObject storedV3 = graph.storeObject(v3, Optional.of(tx));
      ChampObject storedV4 = graph.storeObject(v4, Optional.of(tx));
    
      // Now, within the same transactional context, do a replacement against one of the 
      // vertices that we just created.
      ChampObject replacedV2 = graph.replaceObject(ChampObject.create()
                                                       .ofType("foo")
                                                       .withKey(storedV2.getKey().get())
                                                       .withProperty("replacedProperty3", "replacedValue3")
                                                       .withProperty("replacedProperty4", "replacedValue4")
                                                       .build(), 
                                                   Optional.of(tx));
      
      // Within the same transactional context, do an update against one of the vertices
      // that we just created.
      final ChampObject updatedV3 = graph.storeObject(ChampObject.create()
                                                        .from(storedV3)
                                                        .withKey(storedV3.getKey().get())
                                                        .withProperty("updatedProperty", "updatedValue")
                                                        .build(), Optional.of(tx));
      
      // Within the same transactional context, delete one of the vertices that we just
      // created.
      graph.deleteObject(storedV4.getKey().get(), Optional.of(tx));
      
    // Finally, commit our transaction.
    tx.commit();
    
    tx = graph.openTransaction();
    
      Optional<ChampObject> retrievedV1 = graph.retrieveObject(storedV1.getKey().get(), Optional.of(tx));
      assertTrue(retrievedV1.isPresent());
      assertTrue(retrievedV1.get().getProperty("property1").get().equals("value1"));
      assertTrue(retrievedV1.get().getProperty("property2").get().equals("value2"));

      
      Optional<ChampObject> retrievedV2 = graph.retrieveObject(storedV2.getKey().get(), Optional.of(tx));
      assertTrue(retrievedV2.isPresent());
      assertTrue(retrievedV2.get().getProperty("replacedProperty3").get().equals("replacedValue3"));
      assertTrue(retrievedV2.get().getProperty("replacedProperty4").get().equals("replacedValue4"));
      assertFalse(retrievedV2.get().getProperty("value3").isPresent());
      assertFalse(retrievedV2.get().getProperty("value4").isPresent());
      
      Optional<ChampObject> retrievedV3 = graph.retrieveObject(storedV3.getKey().get(), Optional.of(tx));
      assertTrue(retrievedV3.isPresent());
      assertTrue(retrievedV3.get().getProperty("property5").get().equals("value5"));
      assertTrue(retrievedV3.get().getProperty("property6").get().equals("value6"));
      assertTrue(retrievedV3.get().getProperty("updatedProperty").get().equals("updatedValue"));

      
      Optional<ChampObject> retrievedV4 = graph.retrieveObject(storedV4.getKey().get(), Optional.of(tx));
      assertFalse("Deleted vertex should not be present in graph", retrievedV4.isPresent());
      
    tx.commit();
  }
  

  /**
   * This test validates that multiple threads can each open their own transactions with the 
   * graph data store, and that there is no leakage between each thread's transactions.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws ChampUnmarshallingException
   * @throws ChampTransactionException 
   */
  @Test
  public void multipleTransactionTest() throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampUnmarshallingException, ChampTransactionException {
    
    ChampObject v1 = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property1", "value1")
        .withProperty("property2", "value2")
        .build();
    
    ChampObject v2 = ChampObject.create()
        .ofType("bar")
        .withoutKey()
        .withProperty("property3", "value3")
        .withProperty("property4", "value4")
        .build();
       
    // Instantiate and start our two transactional worker threads...
    Thread thread1 = new Thread(new VertexWriter(v1, 0));
    Thread thread2 = new Thread(new VertexWriter(v2, 1));
    thread1.start();
    thread2.start();
    
    // and wait for the threads to complete.
    try {
      thread1.join();
      thread2.join();
      
    } catch (InterruptedException e) { }
        
    // Now that all of our data has been committed, let's open a new transaction
    // and verify that all of our vertices can be retrieved.
    ChampTransaction tx3 = graph.openTransaction();

    Optional<ChampObject> retrievedV1 = graph.retrieveObject(storedVertices[0].getKey().get(), Optional.of(tx3));
    assertTrue(retrievedV1.isPresent());
    Optional<ChampObject> retrievedV2 = graph.retrieveObject(storedVertices[1].getKey().get(), Optional.of(tx3));
    assertTrue(retrievedV2.isPresent());
    
    tx3.commit();
  }

  
  /**
   * This method validates that edges can be successfully created within a single transaction.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampTransactionException 
   */
  @Test
  public void edgeTest() throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampUnmarshallingException, ChampRelationshipNotExistsException, ChampTransactionException {
    
    // Create the source and target vertices for our edge.
    final ChampObject source = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .withProperty("property1", "value1")
        .build();

    final ChampObject target = ChampObject.create()
        .ofType("foo")
        .withoutKey()
        .build();

    // Open a transaction with the graph data store.
    ChampTransaction tx = graph.openTransaction();
    
      // Now, create our vertices.
      ChampObject storedSource = graph.storeObject(source, Optional.of(tx));
      ChampObject storedTarget = graph.storeObject(target, Optional.of(tx));
      
      // Create the edge between the vertices.
      ChampRelationship relationship = new ChampRelationship.Builder(storedSource, storedTarget, "relationship")
          .property("property-1", "value-1")
          .property("property-2", 3)
          .build();
      ChampRelationship storedRelationship = graph.storeRelationship(relationship, Optional.of(tx));
      
      // Validate that we can read back the edge within the transactional context.
      Optional<ChampRelationship> retrievedRelationship = graph.retrieveRelationship(storedRelationship.getKey().get(), Optional.of(tx));
      assertTrue("Failed to retrieve stored relationship", retrievedRelationship.isPresent());
      
    // Commit our transaction.
    graph.commitTransaction(tx);
    
    // Now, open a new transaction.
    tx = graph.openTransaction();
    
      // Now, read back the edge that we just created again, validating that it was
      // successfully committed to the graph.
      retrievedRelationship = graph.retrieveRelationship(storedRelationship.getKey().get(), Optional.of(tx));
      assertTrue("Failed to retrieve stored relationship", retrievedRelationship.isPresent());

    graph.commitTransaction(tx);
  }
  
  private class VertexWriter implements Runnable {
    
    ChampObject vertex;
    int         index;
    
    public VertexWriter(ChampObject vertex, int index) {
      this.vertex = vertex;
      this.index  = index;
    }
    
    public void run() {
      
      ChampTransaction tx=null;
      try {
        
        // Open a threaded transaction to do some work in.
        tx = graph.openTransaction();
        
        // Now store one of our two vertices within the context of this transaction.
        storedVertices[index] = graph.storeObject(vertex, Optional.of(tx));
        
        // Use our latch to indicate that we are done creating vertices, and wait for
        // the other thread to do the same.          
        latch.countDown();
        latch.await();
        
        // Validate that the vertex we created is visible to us in the graph, but the
        // one that the other thread created is not.          
        Optional<ChampObject> retrievedV2 = graph.retrieveObject(storedVertices[index].getKey().get(), Optional.of(tx));
        assertTrue(retrievedV2.isPresent());
        Optional<ChampObject> retrievedV1 = graph.retrieveObject(storedVertices[(index+1)%2].getKey().get(), Optional.of(tx));
        assertFalse(retrievedV1.isPresent());
                
      } catch (InterruptedException | 
               ChampUnmarshallingException | 
               ChampMarshallingException | 
               ChampSchemaViolationException | 
               ChampObjectNotExistsException | ChampTransactionException e) {
        
        fail("Thread failed to interact with graph due to " + e.getMessage());
      
      } finally {
        
        try {
          Thread.sleep(index * 500);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        // Now, commit our transaction and bail...          
        try {
          graph.commitTransaction(tx);
        } catch (ChampTransactionException e) {

        }
      }
    }
  };
}

