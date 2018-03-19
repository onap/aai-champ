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
package org.onap.aai.champcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.onap.aai.champcore.event.ChampEvent;
import org.onap.aai.champcore.exceptions.ChampTransactionException;

/**
 * This class defines the interface for a graph transaction object.
 */
public abstract class ChampTransaction {

  /** Unique identifier for this transaction (largely for logging purposes). */
  protected UUID  id;
  
  protected List<ChampEvent> eventList = Collections.synchronizedList(new ArrayList<ChampEvent>());
  
  public ChampTransaction() {
    
    // Create a unique identifier for this transaction.
    id = UUID.randomUUID();
  }
  
  public String id() {
    return id.toString();
  }
  
  public void logEvent(ChampEvent event) {
    eventList.add(event);
  }
  
  public List<ChampEvent> getEnqueuedEvents() {
    return eventList;
  }
  
  /**
   * Finalize all updates to the graph which have been made within the context
   * of this transaction.
   */
  public abstract void commit() throws ChampTransactionException ;
  
  
  /**
   * Aborts all graph changes made within the context of this transaction, backing
   * out all changes as if they had never happened.
   */
  public abstract void rollback() throws ChampTransactionException ;
  
}
