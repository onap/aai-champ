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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.champ.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.champ.event.GraphEvent;
import org.onap.champ.event.GraphEvent.GraphEventResult;
import org.onap.champ.service.ChampThreadFactory;
import org.onap.champ.service.logging.ChampMsgs;

import org.onap.aai.event.api.EventPublisher;

public class ChampAsyncResponsePublisher {

  private EventPublisher asyncResponsePublisher;

  /**
   * Number of events that can be queued up.
   */
  private Integer responsePublisherQueueSize;

  /**
   * Number of event publisher worker threads.
   */
  private Integer responsePublisherPoolSize;

  /**
   * Internal queue where outgoing events will be buffered.
   **/
  private BlockingQueue<GraphEvent> responsePublisherEventQueue;

  /**
   * Pool of worker threads that do the work of publishing the events to the
   * event bus.
   */
  private ThreadPoolExecutor responsePublisherPool;

  private static final Integer DEFAULT_ASYNC_RESPONSE_PUBLISH_QUEUE_CAPACITY = 10000;

  private static final Integer DEFAULT_ASYNC_RESPONSE_PUBLISH_THREAD_POOL_SIZE = 10;
  private static final String CHAMP_GRAPH_RESPONSE_PUBLISH_THREAD_NAME = "ChampAsyncGraphResponseEventPublisher";

  private static Logger logger = LoggerFactory.getInstance().getLogger(ChampAsyncRequestProcessor.class.getName());

  public ChampAsyncResponsePublisher(EventPublisher asyncResponsePublisher, Integer responsePublisherQueueSize,
      Integer responsePublisherPoolSize) {
    this.responsePublisherQueueSize = responsePublisherQueueSize;

    this.responsePublisherPoolSize = responsePublisherPoolSize;

    responsePublisherEventQueue = new ArrayBlockingQueue<GraphEvent>(responsePublisherQueueSize);
    responsePublisherPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(responsePublisherPoolSize,
        new ChampThreadFactory(CHAMP_GRAPH_RESPONSE_PUBLISH_THREAD_NAME));

    for (int i = 0; i < responsePublisherPoolSize; i++) {
      responsePublisherPool.submit(new GizmoResponsePublisherWorker());
    }
    this.asyncResponsePublisher = asyncResponsePublisher;

    logger.info(ChampMsgs.CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO,
        "ChampAsyncResponsePublisher initialized SUCCESSFULLY! with event publisher "
            + asyncResponsePublisher.getClass().getName());
  }

  public ChampAsyncResponsePublisher(EventPublisher asyncResponsePublisher) {
    responsePublisherQueueSize = DEFAULT_ASYNC_RESPONSE_PUBLISH_QUEUE_CAPACITY;

    responsePublisherPoolSize = DEFAULT_ASYNC_RESPONSE_PUBLISH_THREAD_POOL_SIZE;

    responsePublisherEventQueue = new ArrayBlockingQueue<GraphEvent>(responsePublisherQueueSize);
    responsePublisherPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(responsePublisherPoolSize,
        new ChampThreadFactory(CHAMP_GRAPH_RESPONSE_PUBLISH_THREAD_NAME));

    for (int i = 0; i < responsePublisherPoolSize; i++) {
      responsePublisherPool.submit(new GizmoResponsePublisherWorker());
    }
    this.asyncResponsePublisher = asyncResponsePublisher;

    logger.info(ChampMsgs.CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO,
        "CrudAsyncResponsePublisher initialized SUCCESSFULLY! with event publisher "
            + asyncResponsePublisher.getClass().getName());
  }

  public void publishResponseEvent(GraphEvent event) {
    responsePublisherEventQueue.offer(event);

  }

  private class GizmoResponsePublisherWorker implements Runnable {

    @Override
    public void run() {

      while (true) {

        GraphEvent event = null;
        try {

          // Get the next event to be published from the queue.
          event = responsePublisherEventQueue.take();

        } catch (InterruptedException e) {

          // Restore the interrupted status.
          Thread.currentThread().interrupt();
        }
        // Publish the response

        try {
          event.setTimestamp(System.currentTimeMillis());
          asyncResponsePublisher.sendSync(event.toJson());
          if (event.getResult().equals(GraphEventResult.SUCCESS)) {
            logger.info(ChampMsgs.CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO,
                "Response published for Event of type: " + event.getObjectType() + " with key: " + event.getObjectKey()
                    + " , transaction-id: " + event.getTransactionId() + " , operation: "
                    + event.getOperation().toString() + " , result: " + event.getResult());
          } else {
            logger.info(ChampMsgs.CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO,
                "Response published for Event of type: " + event.getObjectType() + " with key: " + event.getObjectKey()
                    + " , transaction-id: " + event.getTransactionId() + " , operation: "
                    + event.getOperation().toString() + " , result: " + event.getResult() + " , error: "
                    + event.getErrorMessage());
          }
        } catch (Exception ex) {
          logger.error(ChampMsgs.CHAMP_ASYNC_RESPONSE_PUBLISHER_ERROR, ex.getMessage());
        }

      }
    }
  }

}
