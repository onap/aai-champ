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
package org.onap.aai.champcore.graph.impl;

import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TinkerpopTransaction extends ChampTransaction {

  private static final int COMMIT_RETRY_COUNT = 3;
  
  /** Threaded Tinkerpop transaction. */
  protected Graph threadedTransaction;

  
  private static final Logger LOGGER = LoggerFactory.getLogger(TinkerpopTransaction.class);

  protected TinkerpopTransaction() { }
  
  
  /**
   * Creates a new transaction instance.
   * 
   * @param aGraphInstance - Instance of the graph to request the transaction from.
   */
  public TinkerpopTransaction(Graph aGraphInstance) {
    super();
    
    if(!aGraphInstance.features().graph().supportsTransactions()) {
      throw new UnsupportedOperationException();
    }
  
    // Request a threaded transaction object from the graph.
    this.threadedTransaction = aGraphInstance.tx().createThreadedTx();
    
    LOGGER.info("Open transaction - id: " + id);
  }
  
  @Override
  public String id() {
    return id.toString();
  }
  
  public Graph getGraphInstance() {
    return threadedTransaction;
  }

  @Override
  public void commit() throws ChampTransactionException {
    
    LOGGER.debug("Commiting transaction - " + id); 
    
    final long initialBackoff = (int) (Math.random() * 50);

    // If something goes wrong, we will retry a couple of times before
    // giving up.
    for (int i = 0; i < COMMIT_RETRY_COUNT; i++) {
          
      try {
        
        // Do the commit.
        threadedTransaction.tx().commit();
        LOGGER.info("Committed transaction - id: " + id);
        return;
        
      } catch (Throwable e) {
        
        LOGGER.debug("Transaction " + id + " failed to commit due to: " + e.getMessage());
        
        // Have we used up all of our retries?
        if (i == COMMIT_RETRY_COUNT - 1) {
          
          LOGGER.error("Maxed out commit attempt retries, client must handle exception and retry", e);
          threadedTransaction.tx().rollback();
          throw new ChampTransactionException(e);
        }

        // Calculate how long we will wait before retrying...
        final long backoff = (long) Math.pow(2, i) * initialBackoff;
        LOGGER.warn("Caught exception while retrying transaction commit, retrying in " + backoff + " ms");
          
        // ...and sleep before trying the commit again.
        try {
          Thread.sleep(backoff);
          
        } catch (InterruptedException ie) {
          
          LOGGER.info("Interrupted while backing off on transaction commit");
          Thread.interrupted();
          return;
        }
      }
    }
  }

  @Override
  public void rollback() throws ChampTransactionException {
      
    long initialBackoff = (int) (Math.random() * 50);

    
    // If something goes wrong, we will retry a couple of times before
    // giving up.
    for (int i = 0; i < COMMIT_RETRY_COUNT; i++) {
            
      try {
      
        threadedTransaction.tx().rollback(); 
        LOGGER.info("Rolled back transaction - id: " + id);
        return;
        
      } catch (Throwable e) {
        
        LOGGER.debug("Transaction " + id + " failed to roll back due to: " + e.getMessage());
        
        // Have we used up all of our retries?
        if (i == COMMIT_RETRY_COUNT - 1) {
          
          LOGGER.error("Maxed out rollback attempt retries, client must handle exception and retry", e);
          throw new ChampTransactionException(e);
        }
  
        // Calculate how long we will wait before retrying...
        final long backoff = (long) Math.pow(2, i) * initialBackoff;
        LOGGER.warn("Caught exception while retrying transaction roll back, retrying in " + backoff + " ms");
          
        // ...and sleep before trying the commit again.
        try {
          Thread.sleep(backoff);
          
        } catch (InterruptedException ie) {
          
          LOGGER.info("Interrupted while backing off on transaction rollback");
          Thread.interrupted();
          return;
        }
      }
    }
  }
}
