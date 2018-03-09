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

package org.onap.champ.service;

import java.util.concurrent.TimeUnit;

import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.champ.service.logging.ChampMsgs;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Self expiring Cache to hold request transactionIds . Events are expired
 * automatically after configured interval
 */
public class ChampTransactionCache {
  private static Logger logger = LoggerFactory.getInstance().getLogger(ChampTransactionCache
      .class.getName());

  
  private ChampGraph graphImpl;
  private Cache<String, ChampTransaction> cache;



  public ChampTransactionCache(long txTimeOutInSec,ChampGraph graphImpl) {
    CacheBuilder builder = CacheBuilder.newBuilder().expireAfterWrite(txTimeOutInSec, TimeUnit.SECONDS)
        .removalListener(new RemovalListener() {

          public void onRemoval(RemovalNotification notification) {
            if(notification.getCause()==RemovalCause.EXPIRED){
            logger.info(ChampMsgs.CHAMP_TX_CACHE, "Following transaction: "+notification.getKey()+" is being evicted from cache due to timeout of " + txTimeOutInSec+" seconds");
            try {
              graphImpl.rollbackTransaction((ChampTransaction) notification.getValue());
              logger.info(ChampMsgs.CHAMP_TX_CACHE, "Transaction rolledback successfully :" + notification.getKey());
            } catch (ChampTransactionException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            }
          }
        });
    cache = builder.build();

    this.graphImpl = graphImpl;
    
  }

  public  void put(String txId, ChampTransaction tx) {
    cache.put(txId, tx);

  }

  public  ChampTransaction get(String txId) {
    if (txId==null)
      return null;
    if(cache.getIfPresent(txId)==null){
      //cleanup cache so that removalListener is called
      cache.cleanUp();
    }
    return cache.getIfPresent(txId);
  }

  public  void invalidate(String txId) {
    cache.invalidate(txId);
  }
  


}
