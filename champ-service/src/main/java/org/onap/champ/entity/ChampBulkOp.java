/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.champ.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.util.ChampProperties;
import org.onap.champ.util.ChampServiceConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChampBulkOp {
  private static final Gson gson = new GsonBuilder().create();

  private static final String CREATED_TS_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_CREATED_TS_NAME);
  private static final String LAST_MOD_TS_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_LAST_MOD_TS_NAME);


  private String operation;
  private String id;
  private String type;
  private String label;
  private String source;
  private String target;
  private Map<String, Object> properties;


  public String toJson() {
    return gson.toJson(this);
  }

  public static ChampBulkOp fromJson(String jsonString) {
    return gson.fromJson(jsonString, ChampBulkOp.class);
  }

  public ChampObject toChampObject() throws ChampServiceException {
    if (type == null) {
      throw new ChampServiceException("Error constructing object from: " + toJson(), Status.INTERNAL_SERVER_ERROR);
    }

    ChampObject.Builder builder = new ChampObject.Builder(type);

    if (id != null) {
      builder = builder.key(id);
    }
    if (properties != null) {

      //remove the create/updated timestamps as it cause mismatch issue while updating from graph
      Map<String, Object> champProperties = properties.entrySet().stream()
              .filter(x -> !(x.getKey().equals(CREATED_TS_NAME) || x.getKey().equals(LAST_MOD_TS_NAME)))
              .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

      builder = builder.properties(champProperties);
    }

    return builder.build();
  }

  public ChampRelationship toChampRelationship() throws ChampServiceException {
    if ( (type == null) || (source == null) || (target == null) ) {
      throw new ChampServiceException("Error constructing relationship from: " + toJson(), Status.INTERNAL_SERVER_ERROR);
    }

    ChampObject srcObj = new ChampObject.Builder("").key(source).build();
    ChampObject targetObj = new ChampObject.Builder("").key(target).build();
    ChampRelationship.Builder builder = new ChampRelationship.Builder(srcObj, targetObj, type);

    if (id != null) {
      builder = builder.key(id);
    }
    if (properties != null) {
      builder = builder.properties(properties);
    }

    return builder.build();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public void setProperty(String key, String value) {
    if (properties == null) {
      properties = new HashMap<String,Object>();
    }

    properties.put(key, value);
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }
}
