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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;


public class ChampBulkResponse {

  private List<ChampBulkVertexResponse> objects = new ArrayList<ChampBulkVertexResponse>();
  private List<ChampBulkEdgeResponse> relationships = new ArrayList<ChampBulkEdgeResponse>();

  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public String toJson() {
    return gson.toJson(this);
  }

  public static ChampBulkResponse fromJson(String payload) {
    return gson.fromJson(payload, ChampBulkResponse.class);
  }

  public List<ChampBulkVertexResponse> getObjects() {
    return objects;
  }

  public void setObjects(List<ChampBulkVertexResponse> objects) {
    this.objects = objects;
  }

  public List<ChampBulkEdgeResponse> getRelationships() {
    return relationships;
  }

  public void setRelationships(List<ChampBulkEdgeResponse> relationships) {
    this.relationships = relationships;
  }

}
