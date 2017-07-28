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
package org.openecomp.aai.champ.event;


import java.io.IOException;

import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampObjectIndex;
import org.openecomp.aai.champ.model.ChampPartition;
import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.ChampRelationshipIndex;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ChampEvent {

  public enum ChampOperation {
    STORE,
    REPLACE,
    DELETE
  }
  
  private static ObjectMapper mapper = new ObjectMapper();
  
  private ChampOperation         operation;
  private long                   timestamp;
  private ChampObject            vertex            = null;
  private ChampRelationship      relationship      = null;
  private ChampPartition         partition         = null;
  private ChampObjectIndex       objectIndex       = null;
  private ChampRelationshipIndex relationshipIndex = null;
  
  
  public static Builder builder() {
    return new Builder();
  }
  
  public ChampOperation getOperation() {
    return operation;
  }
  
  public void setOperation(ChampOperation operation) {
    this.operation = operation;
  }
    
  public long getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  
  public ChampObject getVertex() {
    return vertex;
  }
  
  public void setVertex(ChampObject vertex) {
    this.vertex = vertex;
  }
  
  public ChampRelationship getRelationship() {
    return relationship;
  }
  
  public void setRelationship(ChampRelationship relationship) {
    this.relationship = relationship;
  }
  
  public ChampPartition getPartition() {
    return partition;
  }
  
  public void setPartition(ChampPartition partition) {
    this.partition = partition;
  }
  
  public ChampObjectIndex getObjectIndex() {
    return objectIndex;
  }
  
  public void setObjectIndex(ChampObjectIndex index) {
    this.objectIndex = index;
  }
  
  public ChampRelationshipIndex getRelationshipIndex() {
    return relationshipIndex;
  }
  
  public void setRelationshipIndex(ChampRelationshipIndex relationshipIndex) {
    this.relationshipIndex = relationshipIndex;
  }
  
  public String toJson() {
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "Unmarshallable: " + e.getMessage();
    }
  }
  
  public static ChampEvent fromJson(String json) throws JsonParseException, JsonMappingException, IOException {

    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper.readValue(json, ChampEvent.class);
  }
  @Override
  public String toString() {
    return toJson();
  }
  
  public static class Builder {
    
    ChampEvent event = null;
    
    
    public Builder() {
      event = new ChampEvent();
    }
    
    public Builder operation(ChampOperation operation) {
      event.setOperation(operation);
      return this;
    }
    
    public Builder entity(ChampObject entity) {
      event.setVertex(entity);
      return this;
    }
    
    public Builder entity(ChampRelationship relationship) {
      event.relationship = relationship;
      return this;
    }
    
    public Builder entity(ChampPartition partition) {
      event.partition = partition;
      return this;
    }
    
    public Builder entity(ChampObjectIndex index) {
      event.objectIndex = index;
      return this;
    }
    
    public Builder entity(ChampRelationshipIndex relationshipIndex) {
      event.relationshipIndex = relationshipIndex;
      return this;
    }
   
    public ChampEvent build() {
      
      event.setTimestamp(System.currentTimeMillis());
      return event;
    }
  }
}
