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
package org.onap.champ.event;

import javax.ws.rs.core.Response.Status;

import com.google.gson.annotations.SerializedName;

public class GraphEvent {

  public enum GraphEventOperation {
    CREATE, UPDATE, DELETE
  }

  public enum GraphEventResult {
    SUCCESS, FAILURE
  }

  private GraphEventOperation operation;

  @SerializedName("transaction-id")
  private String transactionId;

  @SerializedName("database-transaction-id")
  private String dbTransactionId;
  
  private long timestamp;

  private GraphEventVertex vertex;

  private GraphEventEdge edge;

  private GraphEventResult result;

  @SerializedName("error-message")
  private String errorMessage;

  private Status httpErrorStatus;

  public static Builder builder(GraphEventOperation operation) {
    return new Builder(operation);
  }

  public GraphEventOperation getOperation() {
    return operation;
  }

  public String getTransactionId() {
    return transactionId;
  }
  
  public String getDbTransactionId() {
    return dbTransactionId;
  }
  
  public void setDbTransactionId(String id) {
    dbTransactionId = id;
  }
  
  public long getTimestamp() {
    return timestamp;
  }

  public GraphEventVertex getVertex() {
    return vertex;
  }

  public GraphEventEdge getEdge() {
    return edge;
  }

  public GraphEventResult getResult() {
    return result;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setResult(GraphEventResult result) {
    this.result = result;
  }


  public Status getHttpErrorStatus() {
    return httpErrorStatus;
  }

  public void setHttpErrorStatus(Status httpErrorStatus) {
    this.httpErrorStatus = httpErrorStatus;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public void setVertex(GraphEventVertex vertex) {
    this.vertex = vertex;
  }

  public void setEdge(GraphEventEdge edge) {
    this.edge = edge;
  }

  public String getObjectKey() {
    if (this.getVertex() != null) {
      return this.getVertex().getId();
    } else if (this.getEdge() != null) {
      return this.getEdge().getId();
    }

    return null;
  }

  public String getObjectType() {
    if (this.getVertex() != null) {
      return "vertex->" + this.getVertex().getType();
    } else if (this.getEdge() != null) {
      return "edge->" + this.getEdge().getType();
    }

    return null;
  }

  public static class Builder {

    GraphEvent event = null;

    public Builder(GraphEventOperation operation) {
      event = new GraphEvent();
      event.operation = operation;
    }

    public Builder vertex(GraphEventVertex vertex) {
      event.vertex = vertex;
      return this;
    }

    public Builder edge(GraphEventEdge edge) {
      event.edge = edge;
      return this;
    }

    public Builder result(GraphEventResult result) {
      event.result = result;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      event.errorMessage = errorMessage;
      return this;
    }

    public Builder httpErrorStatus(Status httpErrorStatus) {
      event.httpErrorStatus = httpErrorStatus;
      return this;
    }

    public GraphEvent build() {

      event.timestamp = System.currentTimeMillis();
      event.transactionId = java.util.UUID.randomUUID().toString();

      return event;
    }
  }

}
