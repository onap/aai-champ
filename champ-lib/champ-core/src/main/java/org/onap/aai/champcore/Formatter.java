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
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.champcore;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Formatter {

    private static final EELFLogger LOGGER = EELFManager.getInstance().getLogger(Formatter.class);
    protected JsonParser parser = new JsonParser();
    protected final FormatMapper format;

    public Formatter(FormatMapper format) {
        this.format = format;
    }

    public JsonObject output(List<Object> vertices) {
        Stream<Object> stream = null;
        JsonObject result = new JsonObject();
        JsonArray body = new JsonArray();
        if (vertices.size() >= this.format.parallelThreshold()) {
            stream = vertices.parallelStream();
        } else {
            stream = vertices.stream();
        }

        boolean isParallel = stream.isParallel();
        stream.map((v) -> {
            try {
                return Optional.of(this.format.formatObject(v));
            } catch (Exception var3) {
                LOGGER.warn("Failed to format vertex, returning a partial list", var3);
                return Optional.empty();
            }
        }).forEach((obj) -> {
            if (obj.isPresent()) {
                if (isParallel) {
                    synchronized(body) {
                        body.add((JsonElement)obj.get());
                    }
                } else {
                    body.add((JsonElement)obj.get());
                }
            }

        });
        result.add("results", body);
        return result.getAsJsonObject();
    }
}