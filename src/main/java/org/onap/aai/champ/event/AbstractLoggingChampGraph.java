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
package org.onap.aai.champ.event;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.onap.aai.champ.ChampCapabilities;
import org.onap.aai.champ.ChampGraph;
import org.onap.aai.champ.event.ChampEvent.ChampOperation;
import org.onap.aai.champ.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champ.exceptions.ChampMarshallingException;
import org.onap.aai.champ.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.exceptions.ChampUnmarshallingException;
import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampObjectConstraint;
import org.onap.aai.champ.model.ChampObjectIndex;
import org.onap.aai.champ.model.ChampPartition;
import org.onap.aai.champ.model.ChampRelationship;
import org.onap.aai.champ.model.ChampRelationshipConstraint;
import org.onap.aai.champ.model.ChampRelationshipIndex;
import org.onap.aai.champ.model.ChampSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaPublisher;


/**
 * This class provides the hooks to allow Champ operations to be logged to an event
 * stream.
 */
public abstract class AbstractLoggingChampGraph implements ChampGraph {

  private static final Logger logger = LoggerFactory.getLogger(AbstractLoggingChampGraph.class);
  
  public abstract ChampObject                      executeStoreObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException;
  public abstract ChampObject                      executeReplaceObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException;
  public abstract Optional<ChampObject>            retrieveObject(Object key) throws ChampUnmarshallingException;
  public abstract void                             executeDeleteObject(Object key) throws ChampObjectNotExistsException;
  public abstract Stream<ChampObject>              queryObjects(Map<String, Object> queryParams);
  public abstract ChampRelationship                executeStoreRelationship(ChampRelationship relationship) throws ChampUnmarshallingException, ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException;  
  public abstract ChampRelationship                executeReplaceRelationship(ChampRelationship relationship) throws ChampUnmarshallingException, ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException; 
  public abstract Optional<ChampRelationship>      retrieveRelationship(Object key) throws ChampUnmarshallingException;
  public abstract void                             executeDeleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException;
  public abstract Stream<ChampRelationship>        retrieveRelationships(ChampObject object) throws ChampUnmarshallingException, ChampObjectNotExistsException;
  public abstract Stream<ChampRelationship>        queryRelationships(Map<String, Object> queryParams);
  public abstract ChampPartition                   executeStorePartition(ChampPartition partition) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException;
  public abstract void                             executeDeletePartition(ChampPartition graph);
  public abstract void                             executeStoreObjectIndex(ChampObjectIndex index);
  @Override
  public abstract Optional<ChampObjectIndex>       retrieveObjectIndex(String indexName);
  public abstract Stream<ChampObjectIndex>         retrieveObjectIndices();
  public abstract void                             executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException;
  public abstract void                             executeStoreRelationshipIndex(ChampRelationshipIndex index);
  public abstract Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName);
  public abstract Stream<ChampRelationshipIndex>   retrieveRelationshipIndices();
  public abstract void                             executeDeleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException;
  public abstract void                             storeSchema(ChampSchema schema) throws ChampSchemaViolationException;
  public abstract ChampSchema                      retrieveSchema();
  public abstract void                             updateSchema(ChampObjectConstraint objectConstraint) throws ChampSchemaViolationException;
  public abstract void                             updateSchema(ChampRelationshipConstraint schema) throws ChampSchemaViolationException;
  public abstract void                             deleteSchema();
  public abstract ChampCapabilities                capabilities();

   
  /** Configuration property for setting the comma-separated list of servers to use for
   *  communicating with the event bus. */
  public final static String  PARAM_EVENT_STREAM_HOSTS      = "champ.event.stream.hosts";
  
  /** Configuration property for setting the number of events that we will try to 'batch' 
   *  up before sending them to the event bus. */
  public final static String  PARAM_EVENT_STREAM_BATCH_SIZE = "champ.event.stream.batch-size";
  public final static Integer DEFAULT_EVENT_STREAM_BATCH_SIZE = 1;
  
  /** Configuration property for setting the maximum amount of time to wait for a batch of
   *  outgoing messages to fill up before sending the batch. */
  public final static String  PARAM_EVENT_STREAM_BATCH_TIMEOUT = "champ.event.stream.batch-timeout";
  public final static Integer DEFAULT_EVENT_STREAM_BATCH_TIMEOUT_MS = 500; 
  
  public final static String  PARAM_EVENT_STREAM_PUBLISHER_POOL_SIZE = "champ.event.stream.publisher-pool-size";
  public final static Integer DEFAULT_EVENT_STREAM_PUBLISHER_POOL_SIZE = 100;
  
  /** The event stream topic that we will publish Champ events to. */
  public final static String EVENT_TOPIC = "champRawEvents";
    
  /** Number of events to 'batch up' before actually publishing them to the event bus. */
  private Integer eventStreamBatchSize;
  
  private Integer eventStreamBatchTimeout;
  
  private Integer eventStreamPublisherPoolSize;
  
  /** Comma-separated list of hosts for connecting to the event bus. */
  private String  eventStreamHosts = null;
  
  /** Client used for publishing messages to the event bus. */
  protected CambriaPublisher producer;

  /** Pool of worker threads that do the work of publishing the events to the event bus. */
  protected ThreadPoolExecutor publisherPool;
  
  
  /**
   * Create a new instance of the AbstractLoggingChampGraph.
   * 
   * @param properties - Set of configuration properties for this graph instance.
   */
  protected AbstractLoggingChampGraph(Map<String, Object> properties) {
    
    // Extract the necessary parameters from the configuration properties.
    configure(properties);
      
    // Create the executor pool that will do the work of publishing events to the event bus.
    publisherPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(eventStreamPublisherPoolSize);
    
    // Make sure that we are actually provided a list of hosts for connecting to the event
    // bus before we actually try to do anything useful.
    if(eventStreamHosts == null) {
      
      // We were not supplied a list of event bus hosts, so just bail.
      logger.error("Cannot initialize event stream publisher without at least one event bus host.");
      logger.error("NOTE!! Champ events will NOT be published to the event stream!");
      return;
    }
         
    try {
        
      // Instantiate the producer that we will use to publish events to the event stream.
      setProducer(new CambriaClientBuilders.PublisherBuilder()
                        .usingHosts(eventStreamHosts)
                        .onTopic(EVENT_TOPIC)
                        .limitBatch(eventStreamBatchSize, eventStreamBatchTimeout)
                        .build());
      
    } catch (MalformedURLException | GeneralSecurityException e) {
      
      logger.error("Could not instantiate event stream producer due to: " + e.getMessage());
      logger.error("NOTE: Champ events will NOT be published to the event stream");
      producer = null;
    }
  }

      
  /**
   * Process the configuration properties supplied for this graph instance.
   * 
   * @param properties - Configuration parameters.
   */
  private void configure(Map<String, Object> properties) {
    
    eventStreamBatchSize = 
        (Integer) getProperty(properties, PARAM_EVENT_STREAM_BATCH_SIZE,    DEFAULT_EVENT_STREAM_BATCH_SIZE);
    eventStreamBatchTimeout = 
        (Integer) getProperty(properties, PARAM_EVENT_STREAM_BATCH_TIMEOUT, DEFAULT_EVENT_STREAM_BATCH_TIMEOUT_MS);
    eventStreamPublisherPoolSize = 
        (Integer) getProperty(properties, PARAM_EVENT_STREAM_PUBLISHER_POOL_SIZE, DEFAULT_EVENT_STREAM_PUBLISHER_POOL_SIZE);
    
    if(properties.containsKey(PARAM_EVENT_STREAM_HOSTS)) {
      eventStreamHosts = (String) properties.get(PARAM_EVENT_STREAM_HOSTS);
    } 
  }
  
  public void setProducer(CambriaPublisher aProducer) {
    
    producer = aProducer;
  }
  
  private Object getProperty(Map<String, Object> properties, String property, Object defaultValue) {
    
    if(properties.containsKey(property)) {
      return properties.get(property);
    } else {
      return defaultValue;
    }
  }
  
  @Override
  public void shutdown() {
    
    if(publisherPool != null) {
      publisherPool.shutdown();
      
      try {
        publisherPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {}
    }
    
    if(producer != null) {
      producer.close();
    }
  }
  
  public ChampObject storeObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException {
    
    ChampObject storedObject = executeStoreObject(object);
    
    if(storedObject != null) {
      
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.STORE)
                    .entity(storedObject)
                    .build());
    }
    
    return storedObject;
  }
  
  
  public ChampObject replaceObject(ChampObject object)
      throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException {
    
    ChampObject replacedObject = executeReplaceObject(object);
    
    if(replacedObject != null) {
      
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.REPLACE)
                    .entity(replacedObject)
                    .build());
    }
    
    return replacedObject;
  }
  

  public void deleteObject(Object key) throws ChampObjectNotExistsException {

    // Retrieve the object that we are deleting before it's gone, so that we can 
    // report it to the event stream.
    Optional<ChampObject> objectToDelete = Optional.empty();
    try {
      objectToDelete = retrieveObject(key);
      
    } catch (ChampUnmarshallingException e) {
      logger.error("Unable to generate delete object log: " + e.getMessage());
    }
    
    executeDeleteObject(key);
    
    if(objectToDelete.isPresent()) {
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.DELETE)
                    .entity(objectToDelete.get())
                    .build());
    }
  }
  
  
  public ChampRelationship storeRelationship(ChampRelationship relationship)
      throws ChampUnmarshallingException, ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException {  

    ChampRelationship storedRelationship = executeStoreRelationship(relationship);
    
    if(storedRelationship != null) {
      
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.STORE)
                    .entity(storedRelationship)
                    .build());
    }
    
    return storedRelationship;
  }
  
  
  public ChampRelationship replaceRelationship(ChampRelationship relationship)
      throws ChampUnmarshallingException, ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException { 

    ChampRelationship replacedRelationship = executeReplaceRelationship(relationship);
    
    if(replacedRelationship != null) {
      
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.REPLACE)
                    .entity(replacedRelationship)
                    .build());
    }
    
    return replacedRelationship;
  }
  
  
  public void deleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException {

    executeDeleteRelationship(relationship);
    
    // Update the event stream with the current operation.
    logEvent(ChampEvent.builder()
                  .operation(ChampOperation.DELETE)
                  .entity(relationship)
                  .build());
  }
  
  
  public ChampPartition storePartition(ChampPartition partition) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException {

    ChampPartition storedPartition = executeStorePartition(partition);
    
    if(storedPartition != null) {
      
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.STORE)
                    .entity(storedPartition)
                    .build());
    }
    
    return storedPartition;
  }
  
  
  public void deletePartition(ChampPartition graph) {

    executeDeletePartition(graph);
    
    // Update the event stream with the current operation.
    logEvent(ChampEvent.builder()
                  .operation(ChampOperation.DELETE)
                  .entity(graph)
                  .build());
  }
  
  
  public void storeObjectIndex(ChampObjectIndex index) {

    executeStoreObjectIndex(index);
    
    // Update the event stream with the current operation.
    logEvent(ChampEvent.builder()
                  .operation(ChampOperation.STORE)
                  .entity(index)
                  .build());
  }
  
  
  public void deleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
    
    // Retrieve the index that we are deleting before it's gone, so that we can 
    // report it to the event stream.
    Optional<ChampObjectIndex> indexToDelete = retrieveObjectIndex(indexName);
    
    executeDeleteObjectIndex(indexName);
    
    if(indexToDelete.isPresent()) {
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.DELETE)
                    .entity(indexToDelete.get()) 
                    .build());
    }
  }
  
  
  public void storeRelationshipIndex(ChampRelationshipIndex index) {

    executeStoreRelationshipIndex(index);
    
    // Update the event stream with the current operation.
    logEvent(ChampEvent.builder()
                  .operation(ChampOperation.STORE)
                  .entity(index) 
                  .build());
  }
  
  
  public void deleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException {

    // Retrieve the index that we are deleting before it's gone, so that we can 
    // report it to the event stream.
    Optional<ChampRelationshipIndex> indexToDelete = retrieveRelationshipIndex(indexName);
    
    executeDeleteRelationshipIndex(indexName);
    
    if(indexToDelete.isPresent()) {
      // Update the event stream with the current operation.
      logEvent(ChampEvent.builder()
                    .operation(ChampOperation.DELETE)
                    .entity(indexToDelete.get()) 
                    .build());
    }
  }
  
  
  /**
   * Submits an event to be published to the event stream.
   * 
   * @param anEvent - The event to be published.
   */
  public void logEvent(ChampEvent anEvent) {
    
    if(logger.isDebugEnabled()) {
      logger.debug("Submitting event to be published to the event bus: " + anEvent.toString());
    }
    
    try {
      
      // Try submitting the event to be published to the event bus.
      publisherPool.execute(new EventPublisher(anEvent));
    
    } catch (RejectedExecutionException re) {
      logger.error("Event could not be published to the event bus due to: " + re.getMessage());
      
    } catch (NullPointerException npe) {
      logger.error("Can not publish null event to event bus.");
    }
  }
  
  
  /**
   * This class runs as a background thread and is responsible for pulling Champ events off
   * of the internal queue and publishing them to the event stream.
   */
  private class EventPublisher implements Runnable {
    
    /** Partition key to use when publishing events to the event stream.  We WANT all events
     *  to go to a single partition, so we are just using a hard-coded key for every event. */
    private static final String EVENTS_PARTITION_KEY = "champEventKey";
    
    private ChampEvent event;
    
    public EventPublisher(ChampEvent event) {
      this.event = event;
    }
    
    
    @Override
    public void run() {

      boolean done = false;
      while(!done && !Thread.currentThread().isInterrupted()) {
        try {
          
          // Make sure that we actually have a producer instance to use to publish
          // the event with.
          if(producer != null) {
            
            // Try publishing the event to the event bus.
            producer.send(EVENTS_PARTITION_KEY, event.toJson());
            
          } else if (logger.isDebugEnabled()) {            
            logger.debug("Event bus producer is not instantiated - will not attempt to publish event");
          }
          
          done = true;
          
        } catch (IOException e) {
  
          // We were unable to publish to the event bus, so wait a bit and then try
          // again.
          try {
            Thread.sleep(500);
            
          } catch (InterruptedException e1) {
            logger.info("Stopping event publisher worker thread.");
            return;
          }
        }           
      }
    }
  }
}
