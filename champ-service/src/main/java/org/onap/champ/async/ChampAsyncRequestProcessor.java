/**
 * ﻿============LICENSE_START=======================================================
 * Gizmo
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
 */
package org.onap.champ.async;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response.Status;

import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.champ.ChampRESTAPI;
import org.onap.champ.event.GraphEvent;
import org.onap.champ.event.GraphEvent.GraphEventResult;
import org.onap.champ.event.GraphEventEdge;
import org.onap.champ.event.GraphEventVertex;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.service.ChampDataService;
import org.onap.champ.service.ChampThreadFactory;
import org.onap.champ.service.logging.ChampMsgs;

import org.onap.aai.event.api.EventConsumer;

/**
 * This Class polls the Graph events from request topic perform the necessary
 * CRUD operation by calling champDAO and queues up the response to be consumed
 * by response handler.
 */
public class ChampAsyncRequestProcessor extends TimerTask {

  private Logger logger = LoggerFactory.getInstance().getLogger(ChampAsyncRequestProcessor.class);

  private ChampDataService champDataService;

  /**
   * Number of events that can be queued up.
   */
  private Integer requestProcesserQueueSize;

  /**
   * Number of event publisher worker threads.
   */
  private Integer requestProcesserPoolSize;
  
  /**
   * Number of event publisher worker threads.
   */
  private Integer requestPollingTimeSeconds;

  /**
   * Internal queue where outgoing events will be buffered until they can be
   * serviced by.
   **/
  private BlockingQueue<GraphEvent> requestProcesserEventQueue;

  /**
   * Pool of worker threads that do the work of publishing the events to the
   * event bus.
   */
  private ThreadPoolExecutor requestProcesserPool;

  private ChampAsyncResponsePublisher champAsyncResponsePublisher;

  private EventConsumer asyncRequestConsumer;

  private static final Integer DEFAULT_ASYNC_REQUEST_PROCESS_QUEUE_CAPACITY = 10000;

  private static final Integer DEFAULT_ASYNC_REQUEST_PROCESS_THREAD_POOL_SIZE = 10;
  private static final Integer DEFAULT_ASYNC_REQUEST_PROCESS_POLLING_SECOND = 30000;
  private static final String CHAMP_GRAPH_REQUEST_PROCESS_THREAD_NAME = "ChampAsyncGraphRequestEventProcessor";
  Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(ChampRESTAPI.class.getName());

  public ChampAsyncRequestProcessor(ChampDataService champDataService,
      ChampAsyncResponsePublisher champAsyncResponsePublisher, EventConsumer asyncRequestConsumer) {

    this.requestProcesserQueueSize = DEFAULT_ASYNC_REQUEST_PROCESS_QUEUE_CAPACITY;

    this.requestProcesserPoolSize = DEFAULT_ASYNC_REQUEST_PROCESS_THREAD_POOL_SIZE;

    this.requestPollingTimeSeconds = DEFAULT_ASYNC_REQUEST_PROCESS_POLLING_SECOND;
    requestProcesserEventQueue = new ArrayBlockingQueue<GraphEvent>(requestProcesserQueueSize);
    requestProcesserPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(requestProcesserPoolSize,
        new ChampThreadFactory(CHAMP_GRAPH_REQUEST_PROCESS_THREAD_NAME));

    for (int i = 0; i < requestProcesserPoolSize; i++) {
      requestProcesserPool.submit(new ChampProcessorWorker());
    }

    this.champDataService = champDataService;
    this.champAsyncResponsePublisher = champAsyncResponsePublisher;
    this.asyncRequestConsumer = asyncRequestConsumer;
    logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
        "ChampAsyncRequestProcessor initialized SUCCESSFULLY! with event consumer "
            + asyncRequestConsumer.getClass().getName());
  }
  
  

  public ChampAsyncRequestProcessor(ChampDataService champDataService,
      ChampAsyncResponsePublisher champAsyncResponsePublisher, EventConsumer asyncRequestConsumer,
      Integer requestProcesserQueueSize, Integer requestProcesserPoolSize, Integer requestPollingTimeSeconds) {

    this.requestProcesserQueueSize = requestProcesserQueueSize;

    this.requestProcesserPoolSize = requestProcesserPoolSize;
    
    this.requestPollingTimeSeconds = requestPollingTimeSeconds;

    requestProcesserEventQueue = new ArrayBlockingQueue<GraphEvent>(requestProcesserQueueSize);
    requestProcesserPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(requestProcesserPoolSize,
        new ChampThreadFactory(CHAMP_GRAPH_REQUEST_PROCESS_THREAD_NAME));

    for (int i = 0; i < requestProcesserPoolSize; i++) {
      requestProcesserPool.submit(new ChampProcessorWorker());
    }

    this.champDataService = champDataService;
    this.champAsyncResponsePublisher = champAsyncResponsePublisher;
    this.asyncRequestConsumer = asyncRequestConsumer;
    logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
        "ChampAsyncRequestProcessor initialized SUCCESSFULLY! with event consumer "
            + asyncRequestConsumer.getClass().getName());
  }

  private class ChampProcessorWorker implements Runnable {

    @Override
    public void run() {

      while (true) {

        GraphEvent event = null;
        try {
          // Get the next event to be published from the queue.
          event = requestProcesserEventQueue.take();
        } catch (InterruptedException e) {
          // Restore the interrupted status.
          Thread.currentThread().interrupt();
        }

        // Parse the event and call champ Dao to process , Create the
        // response event and put it on response queue
        event.setResult(GraphEventResult.SUCCESS);
        
        // Check if this request is part of an ongoing DB transaction
        ChampTransaction transaction = champDataService.getTransaction(event.getDbTransactionId());
        if ( (event.getDbTransactionId() != null) && (transaction == null) ) {
          event.setResult(GraphEventResult.FAILURE);
          event.setErrorMessage("Database transactionId " + event.getDbTransactionId() + " not found");
          event.setHttpErrorStatus(Status.BAD_REQUEST);
        }
        
        if (event.getResult() != GraphEventResult.FAILURE) {
          try {
            if (event.getVertex() != null) {

              switch (event.getOperation()) {
              case CREATE:
                event.setVertex(GraphEventVertex.fromChampObject(
                    champDataService.storeObject(event.getVertex().toChampObject(), Optional.ofNullable(transaction)),
                    event.getVertex().getModelVersion()));
                break;

              case UPDATE:
                event.setVertex(GraphEventVertex.fromChampObject(
                    champDataService.replaceObject(event.getVertex().toChampObject(), event.getVertex().getId(), Optional.ofNullable(transaction)),
                    event.getVertex().getModelVersion()));
                break;
              case DELETE:
                champDataService.deleteObject(event.getVertex().getId(), Optional.ofNullable(transaction));
                break;
              default:
                // log error
              }
            } else if (event.getEdge() != null) {
              switch (event.getOperation()) {
              case CREATE:
                event.setEdge(GraphEventEdge.fromChampRelationship(
                    champDataService.storeRelationship(event.getEdge().toChampRelationship(), Optional.ofNullable(transaction)),
                    event.getEdge().getModelVersion()));
                break;

              case UPDATE:
                event.setEdge(GraphEventEdge.fromChampRelationship(champDataService
                    .updateRelationship(event.getEdge().toChampRelationship(), event.getEdge().getId(), Optional.ofNullable(transaction)),
                    event.getEdge().getModelVersion()));

                break;
              case DELETE:
                champDataService.deleteRelationship(event.getEdge().getId(), Optional.ofNullable(transaction));
                break;
              default:
                logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR,
                    "Invalid operation for event transactionId: " + event.getTransactionId());
              }

            } else {
              logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR,
                  "Invalid payload for event transactionId: " + event.getTransactionId());
            }
          } catch (ChampServiceException champException) {
            logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR, champException.getMessage());
            event.setResult(GraphEventResult.FAILURE);
            event.setErrorMessage(champException.getMessage());
            event.setHttpErrorStatus(champException.getHttpStatus());

          } catch (Exception ex) {
            logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR, ex.getMessage());
            event.setResult(GraphEventResult.FAILURE);
            event.setErrorMessage(ex.getMessage());
            event.setHttpErrorStatus(Status.INTERNAL_SERVER_ERROR);
          }
        }

        if (event.getResult().equals(GraphEventResult.SUCCESS)) {
          logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
              "Event processed of type: " + event.getObjectType() + " with key: " + event.getObjectKey()
                  + " , transaction-id: " + event.getTransactionId() + " , operation: "
                  + event.getOperation().toString() + " , result: " + event.getResult());
        } else {
          logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
              "Event processed of type: " + event.getObjectType() + " with key: " + event.getObjectKey()
                  + " , transaction-id: " + event.getTransactionId() + " , operation: "
                  + event.getOperation().toString() + " , result: " + event.getResult() + " , error: "
                  + event.getErrorMessage());
        }

        champAsyncResponsePublisher.publishResponseEvent(event);

      }
    }
  }

  @Override
  public void run() {

    logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO, "Listening for graph events");

    if (asyncRequestConsumer == null) {
      logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR, "Unable to initialize ChampAsyncRequestProcessor");
    }

    Iterable<String> events = null;
    try {
      events = asyncRequestConsumer.consume();
    } catch (Exception e) {
      logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR, e.getMessage());
      return;
    }

    if (events == null || !events.iterator().hasNext()) {
      logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO, "No events recieved");

    }

    for (String event : events) {
      try {
        GraphEvent requestEvent = GraphEvent.fromJson(event);
        auditLogger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
            "Event received of type: " + requestEvent.getObjectType() + " with key: " + requestEvent.getObjectKey()
                + " , transaction-id: " + requestEvent.getTransactionId() + " , operation: "
                + requestEvent.getOperation().toString());
        logger.info(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
            "Event received of type: " + requestEvent.getObjectType() + " with key: " + requestEvent.getObjectKey()
                + " , transaction-id: " + requestEvent.getTransactionId() + " , operation: "
                + requestEvent.getOperation().toString());
        logger.debug(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_INFO, "Event received with payload:" + event);

        // Try to submit the event to be published to the event bus.
        if (!requestProcesserEventQueue.offer(requestEvent)) {
          logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR,
              "Event could not be published to the event bus due to: Internal buffer capacity exceeded.");
        }

      } catch (Exception e) {
        logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR, e.getMessage());
      }
    }

    try {
      asyncRequestConsumer.commitOffsets();
    } catch(OperationNotSupportedException e) {
        //Dmaap doesnt support commit with offset	
        logger.debug(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_WARN, e.getMessage());
    } 
    catch (Exception e) {
      logger.error(ChampMsgs.CHAMP_ASYNC_REQUEST_PROCESSOR_WARN, e.getMessage());
    }

  }



  public Integer getRequestPollingTimeSeconds() {
    return requestPollingTimeSeconds;
  }

  
}
