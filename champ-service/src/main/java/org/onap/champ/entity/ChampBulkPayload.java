/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ================================================================================
 * Modifications Copyright (C) 2019 IBM.
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


public class ChampBulkPayload {

  public static final String ADD_OP = "add";
  public static final String UPDATE_OP = "modify";
  public static final String DELETE_OP = "delete";
  public static final String PATCH_OP = "patch";

  private List<ChampBulkOp> edgeDeleteOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> vertexDeleteOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> vertexAddModifyOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> edgeAddModifyOps = new ArrayList<ChampBulkOp>();

  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public String toJson() {
    return gson.toJson(this);
  }

  public static ChampBulkPayload fromJson(String payload) {
    return gson.fromJson(payload, ChampBulkPayload.class);
  }

  public List<ChampBulkOp> getEdgeDeleteOps() {
    return edgeDeleteOps;
  }

  public void setEdgeDeleteOps(List<ChampBulkOp> ops) {
    this.edgeDeleteOps = ops;
  }

  public List<ChampBulkOp> getVertexDeleteOps() {
    return vertexDeleteOps;
  }

  public void setVertexDeleteOps(List<ChampBulkOp> ops) {
    this.vertexDeleteOps = ops;
  }

  public List<ChampBulkOp> getVertexAddModifyOps() {
    return vertexAddModifyOps;
  }

  public void setVertexAddModifyOps(List<ChampBulkOp> ops) {
    this.vertexAddModifyOps = ops;
  }

  public List<ChampBulkOp> getEdgeAddModifyOps() {
    return edgeAddModifyOps;
  }

  public void setEdgeAddModifyOps(List<ChampBulkOp> ops) {
    this.edgeAddModifyOps = ops;
  }
}
