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

import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampElement;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.util.ChampProperties;
import org.onap.champ.util.ChampServiceConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ChampUUIDService {
  private ChampGraph graphImpl;
  private static final String KEY_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_KEY_NAME);


  public ChampUUIDService(ChampGraph graphImpl) {
    this.graphImpl = graphImpl;
  }

  public List populateUUIDKey(List<ChampElement> elements) {
    {
      List response = new ArrayList();
      for (ChampElement e : elements) {
        ChampElement item = populateUUIDKey(e);
        if (item != null) {
          response.add(item);
        }
      }
      return response;
    }

  }

  public ChampElement populateUUIDKey(ChampElement e) {
    {
      ChampElement response = null;

      if (e.isObject()) {
        if (e.asObject().getProperty(KEY_NAME).isPresent()) {
          response = (ChampObject.create().from(e.asObject())
              .withKey(e.asObject().getProperty(KEY_NAME).get().toString()).build());
        }
      } else {
        if (e.asRelationship().getProperty(KEY_NAME).isPresent()
            && e.asRelationship().getSource().getProperty(KEY_NAME).isPresent()
            && e.asRelationship().getTarget().getProperty(KEY_NAME).isPresent()) {
          ChampObject source = ChampObject.create().from(e.asRelationship().getSource())
              .withKey(e.asRelationship().getSource().getProperty(KEY_NAME).get().toString()).build();
          ChampObject target = ChampObject.create().from(e.asRelationship().getTarget())
              .withKey(e.asRelationship().getTarget().getProperty(KEY_NAME).get().toString()).build();
          ChampRelationship rel = new ChampRelationship.Builder(source, target, e.asRelationship().getType())
              .key(e.asRelationship().getProperty(KEY_NAME).get().toString())
              .properties(e.asRelationship().getProperties()).build();
          response = rel;
        }

      }

      return response;
    }

  }

  public void populateUUIDProperty(ChampElement e, String uuid) {
    e.getProperties().put(KEY_NAME, uuid);
  }


  public Optional<ChampObject> getObjectbyUUID(String uuid, ChampTransaction transaction)
      throws ChampUnmarshallingException, ChampTransactionException, ChampServiceException {
    Optional<ChampObject> response = Optional.empty();

    Stream<ChampObject> s;
    Map<String, Object> filter = new HashMap<>();
    filter.put(KEY_NAME, uuid);

    s = graphImpl.queryObjects(filter, Optional.ofNullable(transaction));
    Object[] objs = s.toArray();
    if (objs.length == 0) {
      return response;
    }
    response = graphImpl.retrieveObject(((ChampObject) objs[0]).getKey().get(), Optional.ofNullable(transaction));
    return response;
  }

  public Optional<ChampRelationship> getRelationshipbyUUID(String uuid, ChampTransaction transaction)
      throws ChampUnmarshallingException, ChampTransactionException, ChampServiceException {
    Optional<ChampRelationship> response = Optional.empty();


    Stream<ChampRelationship> s;
    Map<String, Object> filter = new HashMap<>();
    filter.put(KEY_NAME, uuid);

    s = graphImpl.queryRelationships(filter, Optional.ofNullable(transaction));
    Object[] objs = s.toArray();
    if (objs.length == 0) {
      return response;
    }
    response = graphImpl.retrieveRelationship(((ChampRelationship) objs[0]).getKey().get(),
        Optional.ofNullable(transaction));
    return response;
  }

}
