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


import org.onap.aai.champcore.model.ChampObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChampBulkVertexResponse {
  private static final Gson gson = new GsonBuilder().create();

  private String label;
  private ChampObject vertex;


  public ChampBulkVertexResponse(String label, ChampObject obj) {
    this.label = label;
    this.vertex = obj;
  }

  public String toJson() {
    return gson.toJson(this);
  }

  public static ChampBulkVertexResponse fromJson(String jsonString) {
    return gson.fromJson(jsonString, ChampBulkVertexResponse.class);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ChampObject getVertex() {
    return vertex;
  }

  public void setVertex(ChampObject vertex) {
    this.vertex = vertex;
  }
}
