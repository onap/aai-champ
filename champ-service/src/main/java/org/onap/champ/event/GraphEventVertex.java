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

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.Response.Status;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.champ.entity.ChampObjectDeserializer;
import org.onap.champ.exception.ChampServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

/**
 * This class provides a generic representation of a Vertex as provided by the
 * graph data store.
 */
public class GraphEventVertex {

  /**
   * The unique identifier used to identify this vertex in the graph data
   * store.
   */
  @SerializedName("key")
  private String id;

  @SerializedName("schema-version")
  private String modelVersion;

  /**
   * Type label assigned to this vertex.
   */
  private String type;

  /**
   * Map of all of the properties assigned to this vertex.
   */
  private JsonElement properties;

  /**
   * Marshaller/unmarshaller for converting to/from JSON.
   */
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public GraphEventVertex(String id, String modelVersion, String type, JsonElement properties) {
    this.id = id;
    this.modelVersion = modelVersion;
    this.type = type;
    this.properties = properties;
  }

  public GraphEventVertex() {

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


  public JsonElement getProperties() {
    return properties;
  }

  public void setProperties(JsonElement properties) {
    this.properties = properties;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  /**
   * Unmarshalls this Vertex object into a JSON string.
   *
   * @return - A JSON format string representation of this Vertex.
   */
  public String toJson() {
    return gson.toJson(this);
  }

  /**
   * Marshalls the provided JSON string into a Vertex object.
   *
   * @param json - The JSON string to produce the Vertex from.
   * @return - A Vertex object.
   * @throws SpikeException
   */
  public static GraphEventVertex fromJson(String json) throws ChampServiceException {

    try {

      // Make sure that we were actually provided a non-empty string
      // before we
      // go any further.
      if (json == null || json.isEmpty()) {
        throw new ChampServiceException("Empty or null JSON string.", Status.BAD_REQUEST);
      }

      // Marshall the string into a Vertex object.
      return gson.fromJson(json, GraphEventVertex.class);

    } catch (Exception ex) {
      throw new ChampServiceException("Unable to parse JSON string: ", Status.BAD_REQUEST);
    }
  }

  @Override
  public String toString() {

    return toJson();
  }


  public static GraphEventVertex fromChampObject(ChampObject champObject, String modelVersion) {

    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    JsonObject props = gson.toJsonTree(champObject.getProperties(), mapType).getAsJsonObject();
    GraphEventVertex graphEventVertex = new GraphEventVertex(champObject.getKey().orElse("").toString(),
        modelVersion, champObject.getType(), props);
    return graphEventVertex;

  }


  public ChampObject toChampObject() {
    ChampObject.Builder builder = new ChampObject.Builder(this.getType());
    if(this.getId()!=null && !this.getId().isEmpty()){
      builder.key(this.getId());
    }

    if (this.getProperties() != null) {
      java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> propertiesMap = gson.fromJson(this.getProperties(), mapType);
      for (String key : propertiesMap.keySet()) {
        builder.property(key, propertiesMap.get(key));
      }
    }

    return builder.build();

  }

  /**
   * Uses jackson api to convert the json string into a ChampObject.
   *
   * @param json
   * @return
   * @throws IOException
   */
  // Added this method as gson conversion of timestamp properties(create ts and modified ts) differs from the jackson conversion causing failures.
  public ChampObject toChampObject(String json) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      SimpleModule module = new SimpleModule();
      module.addDeserializer(ChampObject.class, new ChampObjectDeserializer());
      mapper.registerModule(module);
      return mapper.readValue(json, ChampObject.class);
    }

}
