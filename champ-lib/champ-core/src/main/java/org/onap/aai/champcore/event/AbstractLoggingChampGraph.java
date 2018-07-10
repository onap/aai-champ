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


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.event.ChampEvent.ChampOperation;
import org.onap.aai.champcore.event.envelope.ChampEventEnvelope;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class provides the hooks to allow Champ operations to be logged to an event
 * stream.
 */
public abstract class AbstractLoggingChampGraph implements ChampGraph {

  public static final String  PARAM_EVENT_QUEUE_CAPACITY = "champcore.event.stream.buffer.capacity";
  public static final Integer DEFAULT_EVENT_QUEUE_CAPACITY = 10000;
  public static final String  PARAM_EVENT_STREAM_PUBLISHER_POOL_SIZE = "champcore.event.stream.publisher-pool-size";
  public static final Integer DEFAULT_EVENT_STREAM_PUBLISHER_POOL_SIZE = 5;
  public static final String PARAM_EVENT_STREAM_PRODUCER = "champcore.event.stream.publisher";

  /** Pool of worker threads that do the work of publishing the events to the event bus. */
  protected ThreadPoolExecutor publisherPool;

  /** Client used for publishing events to the event bus. */
  protected EventPublisher producer;

  /** Internal queue where outgoing events will be buffered until they can be serviced by
   *  the event publisher worker threads. */
  protected BlockingQueue<ChampEvent> eventQueue;

  /** Number of events that can be queued up for publication before we begin dropping
   *  events. */
  private Integer eventQueueCapacity;

  /** Number of event publisher worker threads. */
  private Integer eventStreamPublisherPoolSize;

  private static final Logger logger = LoggerFactory.getLogger(AbstractLoggingChampGraph.class);


  /**
   * Create a new instance of the AbstractLoggingChampGraph.
   *
   * @param properties - Set of configuration properties for this graph instance.
   */
  protected AbstractLoggingChampGraph(Map<String, Object> properties) {

    // Extract the necessary parameters from the configuration properties.
    configure(properties);

    // Make sure we were passed an event producer as one of our properties, otherwise
    // there is really nothing more we can do...
    if(producer == null) {
      logger.error("No event stream producer was supplied.");
      logger.error("NOTE!! Champ events will NOT be published to the event stream!");
      return;
    }

    // Create the blocking queue that we will use to buffer events that we want
    // published to the event bus.
    eventQueue = new ArrayBlockingQueue<ChampEvent>(eventQueueCapacity);

    // Create the executor pool that will do the work of publishing events to the event bus.
    publisherPool =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(eventStreamPublisherPoolSize,
            new ProducerWorkerThreadFactory());

    try {

      // Start up the producer worker threads.
      for(int i=0; i<eventStreamPublisherPoolSize; i++) {
        publisherPool.submit(new EventPublisherWorker());
      }

    } catch (Exception e) {

      logger.error("Failed to instantiate event stream producer thread due to: '" + e.getMessage() + "'");
      logger.error("NOTE!! Champ events may NOT be published to the event stream!");
      return;
    }
  }



  public abstract Optional<ChampObject>       retrieveObject(Object key) throws ChampUnmarshallingException, ChampTransactionException;
  public abstract Optional<ChampObject>       retrieveObject(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampTransactionException;
  public abstract Stream<ChampObject>         queryObjects(Map<String, Object> queryParams) throws ChampTransactionException;
  public abstract Stream<ChampObject>         queryObjects(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException;
  @Override
  public abstract Optional<ChampRelationship> retrieveRelationship(Object key) throws ChampUnmarshallingException, ChampTransactionException;
  @Override
  public abstract Optional<ChampRelationship> retrieveRelationship(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampTransactionException;
  public abstract Stream<ChampRelationship>   retrieveRelationships(ChampObject object) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException;
  public abstract Stream<ChampRelationship>   retrieveRelationships(ChampObject object, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException;
  public abstract Stream<ChampRelationship>   queryRelationships(Map<String, Object> queryParams) throws ChampTransactionException;

  public abstract Stream<ChampRelationship>   queryRelationships(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException;

  /**
    * Creates or updates a vertex in the graph data store.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its
    * discretion.
    *
    * @param object      - The vertex to be created or updated.
    * @param transaction - Optional transaction context to perform the operation in.
    *
    * @return - The vertex, as created, marshaled as a {@link ChampObject}
    *
    * @throws ChampMarshallingException     - If the {@code object} is not able to be marshalled
    *                                         into the backend representation
    * @throws ChampSchemaViolationException - If the {@code object} violates the constraints specifed
    *                                         by {@link ChampGraph#retrieveSchema}
    * @throws ChampObjectNotExistsException - If {@link org.onap.aai.champcore.model.ChampObject#getKey}
    *                                         is not present or object not found in the graph
    * @throws ChampTransactionException     - If an attempt to commit or rollback the transaction failed.
    */
  public abstract ChampObject executeStoreObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Updates an existing vertex in the graph store.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param object      - The vertex to be created or updated.
   * @param transaction - Optional transaction context to perform the operation in.
   *
   * @return - The updated vertex, marshaled as a {@link ChampObject}
   *
   * @throws ChampMarshallingException     - If the {@code object} is not able to be marshalled into
   *                                         the backend representation
   * @throws ChampSchemaViolationException - If the {@code object} violates the constraints specifed
   *                                         by {@link ChampGraph#retrieveSchema}
   * @throws ChampObjectNotExistsException - If {@link org.onap.aai.champcore.model.ChampObject#getKey}
   *                                         is not present or object not found in the graph
   * @throws ChampTransactionException     - If an attempt to commit or rollback the transaction failed.
   */
  public abstract ChampObject executeReplaceObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Deletes an existing vertex from the graph store.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param key         - The key of the ChampObject in the graph {@link ChampObject#getKey}
   * @param transaction - Optional transaction context to perform the operation in.
   *
   * @throws ChampObjectNotExistsException - If {@link org.onap.aai.champcore.model.ChampObject#getKey}
   *                                         is not present or object not found in the graph
   * @throws ChampTransactionException     - If an attempt to commit or rollback the transaction failed.
   */
  public abstract void executeDeleteObject(Object key, Optional<ChampTransaction> transaction) throws ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Creates or updates an edge in the graph data store.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param relationship - The ChampRelationship that you wish to store in the graph
   * @param transaction  - Optional transaction context to perform the operation in.
   *
   * @return - The {@link ChampRelationship} as it was stored.
   *
   * @throws ChampUnmarshallingException         - If the edge which was created could not be
   *                                               unmarshalled into a ChampRelationship
   * @throws ChampMarshallingException           - If the {@code relationship} is not able to be
   *                                               marshalled into the backend representation
   * @throws ChampObjectNotExistsException       - If either the source or target object referenced
   *                                               by this relationship does not exist in the graph
   * @throws ChampSchemaViolationException       - If the {@code relationship} violates the constraints
   *                                               specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent()
   *                                               but the object cannot be found in the graph
   * @throws ChampTransactionException           - If an attempt to commit or rollback the transaction failed.
   */
  public abstract ChampRelationship executeStoreRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException;

  /**
   * Replaces an existing edge in the graph data store.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param relationship  - The ChampRelationship that you wish to replace in the graph
   * @param transaction   - Optional transaction context to perform the operation in.
   *
   * @return - The {@link ChampRelationship} as it was stored.
   *
   * @throws ChampUnmarshallingException         - If the edge which was created could not be
   *                                               unmarshalled into a ChampRelationship
   * @throws ChampMarshallingException           - If the {@code relationship} is not able to be
   *                                               marshalled into the backend representation
   * @throws ChampSchemaViolationException       - If the {@code relationship} violates the constraints
   *                                               specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent()
   *                                               but the object cannot be found in the graph
   * @throws ChampTransactionException           - If an attempt to commit or rollback the transaction failed.
   */
  public abstract ChampRelationship executeReplaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException;

  /**
   * Removes an edge from the graph data store.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param relationship - The ChampRelationship that you wish to remove from the graph.
   * @param transaction  - Optional transaction context to perform the operation in.
   *
   * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent()
   *                                               but the object cannot be found in the graph
   * @throws ChampTransactionException           - If an attempt to commit or rollback the transaction failed.
   */
  public abstract void executeDeleteRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException, ChampTransactionException;

  /**
   * Create or update a {@link ChampPartition}.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param partition   - The ChampPartition that you wish to create or update in the graph.
   * @param transaction - Optional transaction context to perform the operation in.
   *
   * @return - The {@link ChampPartition} as it was stored.
   *
   * @throws ChampSchemaViolationException       - If the {@code relationship} violates the constraints
   *                                               specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent()
   *                                               but the object cannot be found in the graph
   * @throws ChampMarshallingException           - If the {@code relationship} is not able to be
   *                                               marshalled into the backend representation
   * @throws ChampObjectNotExistsException       - If either the source or target object referenced
   *                                               by this relationship does not exist in the graph
   * @throws ChampTransactionException           - If an attempt to commit or rollback the transaction failed.
   */
  public abstract ChampPartition executeStorePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Removes a partition from the graph.
   * <p>
   * If a transaction context is not provided, then a transaction will be automatically
   * created and committed for this operation only, otherwise, the supplied transaction
   * will be used and it will be up to the caller to commit the transaction at its
   * discretion.
   *
   * @param graph       - The partition to be removed.
   * @param transaction - Optional transaction context to perform the operation in.
   *
   * @throws ChampTransactionException     - If an attempt to commit or rollback the transaction failed.
   */
  public abstract void executeDeletePartition(ChampPartition graph, Optional<ChampTransaction> transaction) throws ChampTransactionException;

  /**
   * Create or update an object index in the graph.
   *
   * @param index       - The object index to be created/updated.
   */
  public abstract void executeStoreObjectIndex(ChampObjectIndex index);
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


  /**
   * Thread factory for the event producer workers.
   */
  private class ProducerWorkerThreadFactory implements ThreadFactory {

    private AtomicInteger threadNumber = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
      return new Thread(r, "champEventStreamPublisher-" + threadNumber.getAndIncrement());
    }
  }


  /**
   * Process the configuration properties supplied for this graph instance.
   *
   * @param properties - Configuration parameters.
   */
  private void configure(Map<String, Object> properties) {

    producer = (EventPublisher) properties.get(PARAM_EVENT_STREAM_PRODUCER);

    eventQueueCapacity =
        (Integer) getProperty(properties, PARAM_EVENT_QUEUE_CAPACITY, DEFAULT_EVENT_QUEUE_CAPACITY);
    eventStreamPublisherPoolSize =
        (Integer) getProperty(properties, PARAM_EVENT_STREAM_PUBLISHER_POOL_SIZE, DEFAULT_EVENT_STREAM_PUBLISHER_POOL_SIZE);
  }


  public void setProducer(EventPublisher aProducer) {

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

      try {
        producer.close();

      } catch (Exception e) {
        logger.error("Failed to stop event stream producer: " + e.getMessage());
      }
    }
  }

  @Override
  public void commitTransaction(ChampTransaction transaction) throws ChampTransactionException {

    try {

      // Commit the transaction.
      transaction.commit();

    } catch (ChampTransactionException e) {

      logger.warn("Events associated with transaction " + transaction.id() + " not generated due to transaction commit failure.");

      List<ChampEvent> enqueuedEvents = transaction.getEnqueuedEvents();
      for(ChampEvent event : enqueuedEvents) {

        logger.debug("Graph event " + event.toString() + " not published.");
      }
      throw e;
    }

    // Now that the transaction has been successfully committed, we need
    // to log the events that were produced within that transaction's
    // context.
    List<ChampEvent> enqueuedEvents = transaction.getEnqueuedEvents();
    for(ChampEvent event : enqueuedEvents) {
      logEvent(event);
    }
  }

  @Override
  public void rollbackTransaction(ChampTransaction transaction) throws ChampTransactionException {

    // Rollback the transaction.
    transaction.rollback();
  }

  @Override
  public ChampObject storeObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {
    return storeObject(object, Optional.empty());
  }

  @Override
  public ChampObject storeObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {

    ChampObject storedObject = executeStoreObject(object, transaction);

    if(storedObject != null) {

      logOrEnqueueEvent(ChampEvent.builder()
                                    .operation(ChampOperation.STORE)
                                    .entity(storedObject)
                                    .build(),
                        transaction);
    }

    return storedObject;
  }

  @Override
  public ChampObject replaceObject(ChampObject object)
      throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {

    return replaceObject(object, Optional.empty());
  }

  @Override
  public ChampObject replaceObject(ChampObject object, Optional<ChampTransaction> transaction)
      throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {

    ChampObject replacedObject = executeReplaceObject(object, transaction);

    if(replacedObject != null) {

      logOrEnqueueEvent(ChampEvent.builder()
                                  .operation(ChampOperation.REPLACE)
                                  .entity(replacedObject)
                                  .build(),
                        transaction);
    }

    return replacedObject;
  }

  @Override
  public void deleteObject(Object key) throws ChampObjectNotExistsException, ChampTransactionException {
    deleteObject(key, Optional.empty());
  }

  @Override
  public void deleteObject(Object key, Optional<ChampTransaction> transaction) throws ChampObjectNotExistsException, ChampTransactionException {

    // Retrieve the object that we are deleting before it's gone, so that we can
    // report it to the event stream.
    Optional<ChampObject> objectToDelete = Optional.empty();
    try {
      objectToDelete = retrieveObject(key, transaction);

    } catch (ChampUnmarshallingException e) {
      logger.error("Unable to generate delete object log: " + e.getMessage());
    }

    executeDeleteObject(key, transaction);

    if(objectToDelete.isPresent()) {
      // Update the event stream with the current operation.
      logOrEnqueueEvent(ChampEvent.builder()
                                  .operation(ChampOperation.DELETE)
                                  .entity(objectToDelete.get())
                                  .build(),
                        transaction);
    }
  }

  @Override
  public ChampRelationship storeRelationship(ChampRelationship relationship)
      throws ChampUnmarshallingException,
             ChampMarshallingException,
             ChampObjectNotExistsException,
             ChampSchemaViolationException,
             ChampRelationshipNotExistsException, ChampTransactionException {
      return storeRelationship(relationship, Optional.empty());
  }

  @Override
  public ChampRelationship storeRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
      throws ChampUnmarshallingException,
             ChampMarshallingException,
             ChampObjectNotExistsException,
             ChampSchemaViolationException,
             ChampRelationshipNotExistsException, ChampTransactionException {

    ChampRelationship storedRelationship = executeStoreRelationship(relationship, transaction);

    if(storedRelationship != null) {

      // Update the event stream with the current operation.
      logOrEnqueueEvent(ChampEvent.builder()
                                  .operation(ChampOperation.STORE)
                                  .entity(storedRelationship)
                                  .build(),
                        transaction);
    }

    return storedRelationship;
  }

  @Override
  public ChampRelationship replaceRelationship(ChampRelationship relationship)
      throws ChampUnmarshallingException,
             ChampMarshallingException,
             ChampSchemaViolationException,
             ChampRelationshipNotExistsException, ChampTransactionException {
    return replaceRelationship(relationship, Optional.empty());
  }

  @Override
  public ChampRelationship replaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
      throws ChampUnmarshallingException,
             ChampMarshallingException,
             ChampSchemaViolationException,
             ChampRelationshipNotExistsException, ChampTransactionException {

    ChampRelationship replacedRelationship = executeReplaceRelationship(relationship, transaction);

    if(replacedRelationship != null) {

      // Update the event stream with the current operation.
      logOrEnqueueEvent(ChampEvent.builder()
                                  .operation(ChampOperation.REPLACE)
                                  .entity(replacedRelationship)
                                  .build(),
                        transaction);
    }

    return replacedRelationship;
  }

  @Override
  public void deleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException, ChampTransactionException {
    deleteRelationship(relationship, Optional.empty());
  }

  @Override
  public void deleteRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException, ChampTransactionException {

    executeDeleteRelationship(relationship, transaction);

    // Update the event stream with the current operation.
    logOrEnqueueEvent(ChampEvent.builder()
                                .operation(ChampOperation.DELETE)
                                .entity(relationship)
                                .build(),
                      transaction);
  }

  @Override
  public ChampPartition storePartition(ChampPartition partition) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException {
    return storePartition(partition, Optional.empty());
  }

  @Override
  public ChampPartition storePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException {

    ChampPartition storedPartition = executeStorePartition(partition, transaction);

    if(storedPartition != null) {

      // Update the event stream with the current operation.
      logOrEnqueueEvent(ChampEvent.builder()
                                  .operation(ChampOperation.STORE)
                                  .entity(storedPartition)
                                  .build(),
                        transaction);
    }

    return storedPartition;
  }

  @Override
  public void deletePartition(ChampPartition graph) throws ChampTransactionException{
    deletePartition(graph, Optional.empty());
  }

  @Override
  public void deletePartition(ChampPartition graph, Optional<ChampTransaction> transaction) throws ChampTransactionException {

    executeDeletePartition(graph, transaction);

    // Update the event stream with the current operation.
    logOrEnqueueEvent(ChampEvent.builder()
                                .operation(ChampOperation.DELETE)
                                .entity(graph)
                                .build(),
                      transaction);
  }

  @Override
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

  private void logOrEnqueueEvent(ChampEvent event, Optional<ChampTransaction> transaction) {

    if(!transaction.isPresent()) {
      // Update the event stream with the current operation.
      logEvent(event);
    } else {

      // when the TransactionID is present, add it to the event payload before logging/enqueing the event.
      event.setDbTransactionId ( transaction.get ().id () );
      transaction.get().logEvent(event);
    }
  }

  /**
   * Submits an event to be published to the event stream.
   *
   * @param anEvent - The event to be published.
   */
  public void logEvent(ChampEvent anEvent) {

    if(eventQueue == null) {
      return;
    }

    logger.info("Log champcore event with transaction id: " + anEvent.getTransactionId() + " to event bus");
    if(logger.isDebugEnabled()) {
      logger.debug("Event payload: " + anEvent.toString());
    }

    // Try to submit the event to be published to the event bus.
    if(!eventQueue.offer(anEvent)) {
      logger.error("Event could not be published to the event bus due to: Internal buffer capacity exceeded.");
    }
  }


  /**
   * This class implements the worker threads for our thread pool which are responsible for
   * pulling the next outgoing event from the internal buffer and forwarding them to the event
   * bus client.
   * <p>
   * Each publish operation is performed synchronously, so that the thread will only move on
   * to the next available event once it has actually published the current event to the bus.
   */
  private class EventPublisherWorker implements Runnable {

    /** Partition key to use when publishing events to the event stream.  We WANT all events
     *  to go to a single partition, so we are just using a hard-coded key for every event. */
    private static final String EVENTS_PARTITION_KEY = "champEventKey";


    @Override
    public void run() {

      while(true) {
        ChampEvent event = null;
        try {

          // Get the next event to be published from the queue.
          event = eventQueue.take();

        } catch (InterruptedException e) {

          // Restore the interrupted status.
          Thread.currentThread().interrupt();
        }

        // Create new envelope containing an event header and ChampEvent
        ChampEventEnvelope eventEnvelope = new ChampEventEnvelope(event);

        // Try publishing the event to the event bus.  This call will block until
        try {
          producer.sendSync(EVENTS_PARTITION_KEY, eventEnvelope.toJson());

        } catch (Exception e) {

          logger.error("Failed to publish event to event bus: " + e.getMessage());
        }
      }
    }
  }
}
