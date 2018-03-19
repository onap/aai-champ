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
package org.onap.aai.champcore.event;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.model.ChampSchema;

import org.onap.aai.event.api.EventPublisher;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;


public class AbstractLoggingChampGraphTest {

  /** Event stream producer stub. */
  private InMemoryPublisher producer;
  
  /** In memory graph for testing purposes. */
  private TestGraph testGraph;
  
  
  /**
   * Perform any setup tasks that need to be done prior to each test.
   */
  @Before
  public void setup() {
    
    // Instantiate an event stream producer stub to use in our tests.
    producer = new InMemoryPublisher();
    
    // Instantiate an 'in-memory' graph for test purposes.
    Map<String, Object> graphProperties = new HashMap<String, Object>();
    graphProperties.put("champcore.event.stream.hosts", "myeventstreamhost");
    graphProperties.put("champcore.event.stream.batch-size", 1);
    graphProperties.put("champcore.event.stream.publisher", producer);
    testGraph = new TestGraph(graphProperties);
  }
  
  
  /**
   * Perform any cleanup that needs to be done after each test.
   * 
   * @throws Exception 
   */
  @After
  public void tearDown() throws Exception {
    
    // Close our stubbed producer and graph.
    producer.close();
    testGraph.shutdown();
  }
  
 
  /**
   * Validates that store/replace/delete operation against vertices result in the expected events
   * being published to the event stream.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampTransactionException 
   */
  @Test
  public void vertexOperationsEmptyTransactionsTest() throws ChampMarshallingException, 
                                                             ChampSchemaViolationException, 
                                                             ChampObjectNotExistsException, 
                                                             InterruptedException, 
                                                             JsonParseException, 
                                                             JsonMappingException, 
                                            IOException, 
                                            ChampTransactionException {
            
    // Create a vertex and store it in the graph data store.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  
    testGraph.storeObject(obj1, Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
  
    
    
    // Create a new vertex based on the one that we already created.
    ChampObject obj2 = ChampObject.create()
        .from(obj1)
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, try doing a replace operation.
    testGraph.replaceObject(obj2, Optional.empty());
    
    
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected REPLACE event.", loggedEventStr.contains("REPLACE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
    
    // Finally, delete the vertex.
    testGraph.deleteObject("123", Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
  }
  
  @Test
  public void vertexOperationsLegacyTest2() throws ChampMarshallingException, 
                                                   ChampSchemaViolationException, 
                                                   ChampObjectNotExistsException, 
                                                   InterruptedException, 
                                                   JsonParseException, 
                                                   JsonMappingException, 
                                                   IOException, 
                                                   ChampTransactionException {
            
    // Create a vertex and store it in the graph data store.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  
    testGraph.storeObject(obj1);
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
  
    
    
    // Create a new vertex based on the one that we already created.
    ChampObject obj2 = ChampObject.create()
        .from(obj1)
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, try doing a replace operation.
    testGraph.replaceObject(obj2);
    
    
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected REPLACE event.", loggedEventStr.contains("REPLACE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
    
    // Finally, delete the vertex.
    testGraph.deleteObject("123");
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not a vertex.", loggedEventStr.contains("vertex"));
  }
  
  /**
   * This test validates that performing vertex operations in the case where the data to be
   * forwarded to the event stream is unavailable results in no event being generated, but
   * does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampTransactionException 
   */
  @Test
  public void vertexOperationsWithNullsTest() throws ChampMarshallingException, 
                                                     ChampSchemaViolationException, 
                                                     ChampObjectNotExistsException, 
                                                     InterruptedException, 
                                                     JsonParseException, 
                                                     JsonMappingException, 
                                                     IOException, ChampTransactionException {
            
    // Setup our test graph to simulate failures to retrieve data from the graph data store.
    testGraph.returnNulls();
    
    // Create a vertex and store it in the graph data store.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  
    testGraph.storeObject(obj1, Optional.empty());

    // Check our simulated event stream to verify that an event log was produced.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store vertex event should not have been logged to the event stream", loggedEventStr);
    
    // Create a new vertex based on the one that we already created.
    ChampObject obj2 = ChampObject.create()
        .from(obj1)
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, try doing a replace operation.
    testGraph.replaceObject(obj2, Optional.empty());
    
    // Check our simulated event stream to see if an event log was not produced.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store vertex event should not have been logged to the event stream", loggedEventStr);
    
    // Finally, delete the vertex.
    testGraph.deleteObject("123", Optional.empty());
    
    // Check our simulated event stream to see if an event log was not produced.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store vertex event should not have been logged to the event stream", loggedEventStr);
  }
  
  
  /**
   * Validates that store/replace/delete operation against edges result in the expected events
   * being published to the event stream.
   *
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampTransactionException 
   */
  @Test
  public void edgeOperationsTest() throws ChampMarshallingException, 
                                          ChampSchemaViolationException, 
                                          ChampObjectNotExistsException, 
                                          InterruptedException, 
                                          JsonParseException, 
                                          JsonMappingException, 
                                          IOException, 
                                          ChampUnmarshallingException, 
                                          ChampRelationshipNotExistsException, ChampTransactionException {
    
    // Create two vertices to act as the end points of our edge.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  

    ChampObject obj2 = ChampObject.create()
        .ofType("bar")
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, create an edge object and write it to the graph data store.
    ChampRelationship rel = new ChampRelationship.Builder(obj1, obj2, "relationship")
        .property("property-1", "value-1")
        .property("property-2", "value-2")
        .build();
    testGraph.storeRelationship(rel, Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not an edge.", loggedEventStr.contains("relationship"));
    
    // Now, create another edge object based on the one we just wrote, and use it to perform
    // a replace operation.
    ChampRelationship rel2 = ChampRelationship.create()
        .from(rel)
        .withKey("123")
        .withProperty("property-3", "value-3")
        .build();
    testGraph.replaceRelationship(rel2, Optional.empty());

    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected REPLACE event.", loggedEventStr.contains("REPLACE"));
    assertTrue("Entity type for store event was not an edge.", loggedEventStr.contains("relationship"));
    
    // Finally, delete our edge.
    testGraph.deleteRelationship(rel2, Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not an edge.", loggedEventStr.contains("relationship"));
  }
  
  
  /**
   * This test validates that performing edge operations in the case where the data to be
   * forwarded to the event stream is unavailable results in no event being generated, but
   * does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampTransactionException 
   */
  @Test
  public void edgeOperationsWithNullsTest() throws ChampMarshallingException, 
                                                   ChampSchemaViolationException, 
                                                   ChampObjectNotExistsException, 
                                                   InterruptedException, 
                                                   JsonParseException, 
                                                   JsonMappingException, 
                                                   IOException, 
                                                   ChampUnmarshallingException, 
                                                   ChampRelationshipNotExistsException, ChampTransactionException {
    
    // Set up our graph to simulate a failure to retrieve some of the data we need to generate
    // events.
    testGraph.returnNulls();
    
    // Create two vertices to act as the endpoints of our edge.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  

    ChampObject obj2 = ChampObject.create()
        .ofType("bar")
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, create an edge object and write it to the graph data store.
    ChampRelationship rel = new ChampRelationship.Builder(obj1, obj2, "relationship")
        .property("property-1", "value-1")
        .property("property-2", "value-2")
        .build();
    testGraph.storeRelationship(rel, Optional.empty());
    
    // Check our simulated event stream to see if an event log was produced.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store edge event should not have been logged to the event stream", loggedEventStr);
        
    // Now, create another edge object based on the one we just wrote, and use it to perform
    // a replace operation.
    ChampRelationship rel2 = ChampRelationship.create()
        .from(rel)
        .withKey("123")
        .withProperty("property-3", "value-3")
        .build();
    testGraph.replaceRelationship(rel2, Optional.empty());
    
    // Check our simulated event stream to see if an event log was produced.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store edge event should not have been logged to the event stream", loggedEventStr);   
  }
  
  
  /**
   * Validates that store/replace/delete operation against partitions result in the expected events
   * being published to the event stream.
   *
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampTransactionException 
   */
  @Test
  public void partitionOperationsTest() throws ChampMarshallingException, 
                                               ChampSchemaViolationException, 
                                               ChampObjectNotExistsException, 
                                               InterruptedException, 
                                               JsonParseException, 
                                               JsonMappingException, 
                                               IOException, 
                                               ChampUnmarshallingException, 
                                               ChampRelationshipNotExistsException, ChampTransactionException {
    
    // Create the vertices and edge objects that we need to create a partition.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  

    ChampObject obj2 = ChampObject.create()
        .ofType("bar")
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    // Now, create an edge object and write it to the graph data store.
    ChampRelationship rel = new ChampRelationship.Builder(obj1, obj2, "relationship")
        .property("property-1", "value-1")
        .property("property-2", "value-2")
        .build();
    
    // Now, create our partition object and store it in the graph.
    ChampPartition partition = ChampPartition.create()
        .withObject(obj1)
        .withObject(obj2)
        .withRelationship(rel)
        .build();
    testGraph.storePartition(partition, Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not a partition.", loggedEventStr.contains("partition"));

    // Now, delete our partition.
    testGraph.deletePartition(partition, Optional.empty());
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not a partition.", loggedEventStr.contains("partition"));
  }
  
  
  /**
   * This test validates that performing partition operations in the case where the data to be
   * forwarded to the event stream is unavailable results in no event being generated, but
   * does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampTransactionException 
   */
  @Test
  public void partitionOperationsWithNullsTest() throws ChampMarshallingException, 
                                          ChampSchemaViolationException, 
                                          ChampObjectNotExistsException, 
                                          InterruptedException, 
                                          JsonParseException, 
                                          JsonMappingException, 
                                          IOException, 
                                          ChampUnmarshallingException, 
                                          ChampRelationshipNotExistsException, ChampTransactionException {
    
    // Set up our graph to simulate a failure to retrieve some of the data we need to generate
    // events.
    testGraph.returnNulls();
    
    // Create all of the objects we need to create a partition, and store the partition
    // in the graph.
    ChampObject obj1 = ChampObject.create()
        .ofType("foo")
        .withKey("123")
        .withProperty("p1", "v1")
        .withProperty("p2", "v2")
        .build();  

    ChampObject obj2 = ChampObject.create()
        .ofType("bar")
        .withKey("123")
        .withProperty("p3", "v3")
        .build();
    
    ChampRelationship rel = new ChampRelationship.Builder(obj1, obj2, "relationship")
        .property("property-1", "value-1")
        .property("property-2", "value-2")
        .build();
    
    ChampPartition partition = ChampPartition.create()
        .withObject(obj1)
        .withObject(obj2)
        .withRelationship(rel)
        .build();
    testGraph.storePartition(partition, Optional.empty());
    
    // Check our simulated event stream to see if an an event log was produced.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Store partition event should not have been logged to the event stream", loggedEventStr);
  }
  
  
  /**
   * Validates that store/replace/delete operation against vertex indexes result in the expected
   * events being published to the event stream.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampIndexNotExistsException
   */
  @Test
  public void indexOperationsTest() throws ChampMarshallingException, 
                                           ChampSchemaViolationException, 
                                           ChampObjectNotExistsException, 
                                           InterruptedException, 
                                           JsonParseException, 
                                           JsonMappingException, 
                                           IOException, 
                                           ChampUnmarshallingException, 
                                           ChampRelationshipNotExistsException, 
                                           ChampIndexNotExistsException {
        
    // Create an index object and store it in the graph.
    ChampObjectIndex objIndex = ChampObjectIndex.create()
        .ofName("myIndex")
        .onType("type")
        .forField("myField")
        .build();
    testGraph.storeObjectIndex(objIndex);
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not a vertex index.", loggedEventStr.contains("objectIndex"));
    
    // Now, delete our partition.
    testGraph.deleteObjectIndex("myIndex");
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not a vertex index.", loggedEventStr.contains("objectIndex"));
  }
  
  /**
   * This test validates that performing index operations in the case where the data to be
   * forwarded to the event stream is unavailable results in no event being generated, but
   * does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampIndexNotExistsException
   */
  @Test
  public void indexOperationsWithNullsTest() throws ChampMarshallingException, 
                                                    ChampSchemaViolationException, 
                                                    ChampObjectNotExistsException, 
                                                    InterruptedException, 
                                                    JsonParseException, 
                                                    JsonMappingException, 
                                                    IOException, 
                                                    ChampUnmarshallingException, 
                                                    ChampRelationshipNotExistsException, 
                                                    ChampIndexNotExistsException {
    
    // Set up our graph to simulate a failure to retrieve some of the data we need to generate
    // events.
    testGraph.returnNulls();
    
    // Create an index object and store it in the graph.
    ChampObjectIndex objIndex = ChampObjectIndex.create()
        .ofName("myIndex")
        .onType("type")
        .forField("myField")
        .build();
    testGraph.storeObjectIndex(objIndex);
    
    // Check our simulated event stream to see if an  an event log was produced.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Now, delete our index.
    testGraph.deleteObjectIndex("myIndex");
    
    // Check our simulated event stream to see if an an event log was produced.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Delete partition event should not have been logged to the event stream", loggedEventStr);
  }
  
  
  /**
   * This test validates that performing relationship index operations in the case where 
   * the data to be forwarded to the event stream is unavailable results in no event being 
   * generated, but does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampIndexNotExistsException
   */
  @Test
  public void relationshipIndexOperationsTest() throws ChampMarshallingException, 
                                                       ChampSchemaViolationException, 
                                                       ChampObjectNotExistsException, 
                                                       InterruptedException, 
                                                       JsonParseException, 
                                                       JsonMappingException, 
                                                       IOException, 
                                                       ChampUnmarshallingException, 
                                                       ChampRelationshipNotExistsException, 
                                                       ChampIndexNotExistsException {
        
    // Create a relationship index object and store it in the graph.
    ChampRelationshipIndex relIndex = ChampRelationshipIndex.create()
        .ofName("myIndex")
        .onType("type")
        .forField("myField")
        .build();
    testGraph.storeRelationshipIndex(relIndex);
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected STORE event.", loggedEventStr.contains("STORE"));
    assertTrue("Entity type for store event was not a relationship index.", loggedEventStr.contains("relationshipIndex"));
    
    // Now, delete our partition.
    testGraph.deleteRelationshipIndex("myIndex");
    
    // Retrieve the next event from the event stream and validate that it is what we expect.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    assertTrue("Expected DELETE event.", loggedEventStr.contains("DELETE"));
    assertTrue("Entity type for store event was not a relationship index.", loggedEventStr.contains("relationshipIndex"));
  }
  
  
  /**
   * This test validates that performing index operations in the case where the data to be
   * forwarded to the event stream is unavailable results in no event being generated, but
   * does not otherwise create issues.
   * 
   * @throws ChampMarshallingException
   * @throws ChampSchemaViolationException
   * @throws ChampObjectNotExistsException
   * @throws InterruptedException
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws ChampUnmarshallingException
   * @throws ChampRelationshipNotExistsException
   * @throws ChampIndexNotExistsException
   */
  @Test
  public void relationshipIndexOperationsWithNullsTest() throws ChampMarshallingException, 
                                                                ChampSchemaViolationException, 
                                                                ChampObjectNotExistsException, 
                                                                InterruptedException, 
                                                                JsonParseException, 
                                                                JsonMappingException, 
                                                                IOException, 
                                                                ChampUnmarshallingException, 
                                                                ChampRelationshipNotExistsException, 
                                                                ChampIndexNotExistsException {
    
    // Set up our graph to simulate a failure to retrieve some of the data we need to generate
    // events.
    testGraph.returnNulls();
    
    // Create a relationship index object and store it in the graph.
    ChampRelationshipIndex relIndex = ChampRelationshipIndex.create()
        .ofName("myIndex")
        .onType("type")
        .forField("myField")
        .build();
    
    testGraph.storeRelationshipIndex(relIndex);
    
    // Check our simulated event stream to see if an an event log was produced.
    String loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Now, delete our index.
    testGraph.deleteRelationshipIndex("myIndex");
    
    // Check our simulated event stream to see if an event log was produced.
    loggedEventStr = producer.eventStream.poll(5000, TimeUnit.MILLISECONDS);
    
    // Validate that we did not get an event from the stream.
    assertNull("Delete partition event should not have been logged to the event stream", loggedEventStr);
  }
      
  
  /**
   * This is a simple graph stub that extends our {@link AbstractLoggingChampGraph} class which 
   * we can use to validate that log events get generated without worrying about having a real
   * underlying graph.
   */
  private class TestGraph extends AbstractLoggingChampGraph {
    
    /** If set, this causes simulated retrieve operations to fail. */
    private boolean returnNulls = false;
    
    
    protected TestGraph(Map<String, Object> properties) {
      super(properties);      
    }

    public void returnNulls() {
      returnNulls = true;
    }
    
    @Override 
    public void shutdown() {
      if(returnNulls) {
        publisherPool = null;
      }
      super.shutdown();
    }
    
    @Override
    public ChampObject executeStoreObject(ChampObject object, Optional<ChampTransaction> transaction) 
        throws ChampMarshallingException,
               ChampSchemaViolationException, 
               ChampObjectNotExistsException {
      
      if(!returnNulls) {
        return object;
      } else {
        return null;
      }
    }

    @Override
    public ChampObject executeReplaceObject(ChampObject object, Optional<ChampTransaction> transaction) 
        throws ChampMarshallingException,
               ChampSchemaViolationException, 
               ChampObjectNotExistsException {
      
      if(!returnNulls) {
        return object;
      } else {
        return null;
      }
    }

    @Override
    public Optional<ChampObject> retrieveObject(Object key) throws ChampUnmarshallingException {
      return retrieveObject(key, Optional.empty());
    }
    
    @Override
    public Optional<ChampObject> retrieveObject(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException {
      
      if(!returnNulls) {
        return(Optional.of(ChampObject.create()
                            .ofType("foo")
                            .withKey(key)
                            .build()));  
      } else {
        return Optional.empty();
      }
    }

    @Override
    public void executeDeleteObject(Object key, Optional<ChampTransaction> transaction) throws ChampObjectNotExistsException {
   
    }

    @Override
    public Stream<ChampObject> queryObjects(Map<String, Object> queryParams) {
      return queryObjects(queryParams, Optional.empty());
    }

   
    @Override
    public Stream<ChampObject> queryObjects(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) {
      // Not used by any tests.
      return null;
    }

    @Override
    public ChampRelationship executeStoreRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) 
        throws ChampUnmarshallingException, 
               ChampMarshallingException, 
               ChampObjectNotExistsException, 
               ChampSchemaViolationException,
               ChampRelationshipNotExistsException {

      if(!returnNulls) {
        return relationship;
      } else {
        return null;
      }
    }

    @Override
    public ChampRelationship executeReplaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
        throws ChampUnmarshallingException, 
               ChampMarshallingException,
               ChampSchemaViolationException, 
               ChampRelationshipNotExistsException {

      if(!returnNulls) {
        return relationship;
      } else {
        return null;
      }
    }

    @Override
    public Optional<ChampRelationship> retrieveRelationship(Object key) throws ChampUnmarshallingException {
      return retrieveRelationship(key, Optional.empty());
    }
    
    @Override
    public Optional<ChampRelationship> retrieveRelationship(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException {
      // Not used by any tests.
      return null;
    }

    @Override
    public void executeDeleteRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException {
      // Not used by any tests.   
    }

    @Override
    public Stream<ChampRelationship> retrieveRelationships(ChampObject object)
        throws ChampUnmarshallingException, ChampObjectNotExistsException {
      return retrieveRelationships(object, Optional.empty());
    }
    
    @Override
    public Stream<ChampRelationship> retrieveRelationships(ChampObject object, Optional<ChampTransaction> transaction)
        throws ChampUnmarshallingException, ChampObjectNotExistsException {
      
      // Not used by any tests.
      return null;
    }

    @Override
    public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams) {
      return queryRelationships(queryParams, Optional.empty());
    }
    
    @Override
    public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) {
      
      // Not used by any tests.
      return null;
    }

    @Override
    public ChampPartition executeStorePartition(ChampPartition partition, Optional<ChampTransaction> transaction) 
        throws ChampSchemaViolationException, 
               ChampRelationshipNotExistsException,
               ChampMarshallingException, 
               ChampObjectNotExistsException {

      if(!returnNulls) {
        return partition;
      } else {
        return null;
      }
    }

    @Override
    public void executeDeletePartition(ChampPartition graph, Optional<ChampTransaction> transaction) {
      // Not used by any tests.     
    }

    @Override
    public void executeStoreObjectIndex(ChampObjectIndex index) {
      // Not used by any tests.    
    }

    @Override
    public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName) {
      
      if(!returnNulls) {
        return Optional.of(ChampObjectIndex.create()
                            .ofName(indexName)
                            .onType("doesnt matter")
                            .forField("doesnt matter")
                            .build());
      } else {
        return Optional.empty();
      }
    }

    @Override
    public Stream<ChampObjectIndex> retrieveObjectIndices() {
      // Not used by any tests.
      return null;
    }

    @Override
    public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
      // Not used by any tests.    
    }

    @Override
    public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {
      // Not used by any tests.  
    }

    @Override
    public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
      if(!returnNulls) {
        return Optional.of(ChampRelationshipIndex.create()
                            .ofName(indexName)
                            .onType("doesnt matter")
                            .forField("doesnt matter")
                            .build());
      } else {
        return Optional.empty();
      }
    }

    @Override
    public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
      // Not used by any tests.
      return null;
    }

    @Override
    public void executeDeleteRelationshipIndex(String indexName)
        throws ChampIndexNotExistsException {
      // Not used by any tests.    
    }

    @Override
    public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
      // Not used by any tests.    
    }

    @Override
    public ChampSchema retrieveSchema() {
      // Not used by any tests.
      return null;
    }

    @Override
    public void updateSchema(ChampObjectConstraint objectConstraint)
        throws ChampSchemaViolationException {
      // Not used by any tests.    
    }

    @Override
    public void updateSchema(ChampRelationshipConstraint schema)
        throws ChampSchemaViolationException {
      // Not used by any tests.     
    }

    @Override
    public void deleteSchema() {
      // Not used by any tests.  
    }

    @Override
    public ChampCapabilities capabilities() {
      // Not used by any tests.
      return null;
    }

    @Override
    public ChampTransaction openTransaction() {
      // Not used by any tests.
      return null;
    }

    @Override
    public void commitTransaction(ChampTransaction transaction) {
      // Not used by any tests.
      
    }

    @Override
    public void rollbackTransaction(ChampTransaction transaction) {
      // Not used by any tests.    
    }
  }
  
  private class InMemoryPublisher implements EventPublisher {

    public BlockingQueue<String> eventStream = new ArrayBlockingQueue<String>(50);
    public BlockingQueue<String> failedMsgs = new ArrayBlockingQueue<String>(10);
    private boolean failMode=false;
    
    
    public void enterFailMode() {
      failMode=true;
    }
    
    @Override
    public int sendSync(String partitionKey, String message) throws Exception {
      
      if(!failMode) {
        eventStream.add(message);
        return 0;
      } else {
        failedMsgs.add(message);
        throw new IOException("nope");
      }
    }
    
    @Override
    public int sendSync(String partitionKey, Collection<String> messages) throws Exception {
      
      for(String msg : messages) {
        if(!failMode) {
          eventStream.add(msg);
          return 0;
        } else {
          failedMsgs.add(msg);
          throw new IOException("nope");
        }
      }
      return 0;
    }
    
    @Override
    public int sendSync(String message) throws Exception {
      if(!failMode) {
        eventStream.add(message);
        return 0;
      } else {
        failedMsgs.add(message);
        throw new IOException("nope");
      }
    }
    
    @Override
    public int sendSync(Collection<String> messages) throws Exception {
      
      for(String msg : messages) {
        if(!failMode) {
          eventStream.add(msg);
          return 0;
        } else {
          failedMsgs.add(msg);
          throw new IOException("nope");
        }
      }
      return 0;
    }
    @Override
    public void sendAsync(String partitionKey, String message) throws Exception {
      if(!failMode) {
        eventStream.add(message);
      } else {
        failedMsgs.add(message);
        throw new IOException("nope");
      }      
    }
    @Override
    public void sendAsync(String partitionKey, Collection<String> messages) throws Exception {
      for(String msg : messages) {
        if(!failMode) {
          eventStream.add(msg);
        } else {
          failedMsgs.add(msg);
          throw new IOException("nope");
        }
      }     
    }
    @Override
    public void sendAsync(String message) throws Exception {
      if(!failMode) {
        eventStream.add(message);
      } else {
        failedMsgs.add(message);
        throw new IOException("nope");
      }      
    }
    @Override
    public void sendAsync(Collection<String> messages) throws Exception {
      for(String msg : messages) {
        if(!failMode) {
          eventStream.add(msg);
        } else {
          failedMsgs.add(msg);
          throw new IOException("nope");
        }
      }    
    }
    
    @Override
    public void close() throws Exception {
      // TODO Auto-generated method stub
      
    }    
  }
}
