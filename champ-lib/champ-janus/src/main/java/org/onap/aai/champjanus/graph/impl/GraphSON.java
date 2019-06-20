/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * Modifications Copyright (C) 2019 IBM
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
package org.onap.aai.champjanus.graph.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.onap.aai.champcore.FormatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GraphSON implements FormatMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphSON.class);
    private final GraphSONMapper mapper;
    private final GraphSONWriter writer;
    protected JsonParser parser;

    public GraphSON() {
        this.mapper = GraphSONMapper.build().addRegistry(JanusGraphIoRegistry.getInstance()).create();
        this.writer = GraphSONWriter.build().mapper(this.mapper).create();
        this.parser = new JsonParser();
    }

    public JsonObject formatObject(Object v) {
        OutputStream os = new ByteArrayOutputStream();
        String result = "";

        try {
            this.writer.writeVertex(os, (Vertex) v, Direction.BOTH);
            result = os.toString();
        } catch (IOException var5) {
            LOGGER.debug("Exception occured while formatting object : " + var5.getMessage());
        }

        return this.parser.parse(result).getAsJsonObject();
    }

    public int parallelThreshold() {
        return 50;
    }
}
