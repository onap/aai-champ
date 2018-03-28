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
package org.onap.champ.event.envelope;

import javax.ws.rs.core.Response.Status;
import org.onap.champ.event.GraphEvent;
import org.onap.champ.exception.ChampServiceException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GraphEventEnvelope {

    private GraphEventHeader header;
    private GraphEvent body;

    /**
     * Serializer/deserializer for converting to/from JSON.
     */
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public GraphEventEnvelope(GraphEvent event) {
        this.header = new GraphEventHeader.Builder().requestId(event.getTransactionId()).build();
        this.body = event;
    }

    public GraphEventEnvelope(GraphEventHeader header, GraphEvent body) {
        this.header = header;
        this.body = body;
    }

    public GraphEventHeader getHeader() {
        return header;
    }

    public void setHeader(GraphEventHeader header) {
        this.header = header;
    }

    public GraphEvent getBody() {
        return body;
    }

    public void setBody(GraphEvent body) {
        this.body = body;
    }

    /**
     * Serializes this Vertex object into a JSON string.
     *
     * @return - A JSON format string representation of this Vertex.
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Deserializes the provided JSON string into a Event Envelope object.
     *
     * @param json the JSON string to produce the Event Envelope from.
     * @return an Event Envelope object.
     * @throws ChampServiceException
     */
    public static GraphEventEnvelope fromJson(String json) throws ChampServiceException {
        try {
            if (json == null || json.isEmpty()) {
                throw new ChampServiceException("Empty or null JSON string.", Status.BAD_REQUEST);
            }
            return gson.fromJson(json, GraphEventEnvelope.class);
        } catch (Exception ex) {
            throw new ChampServiceException("Unable to parse JSON string: ", Status.BAD_REQUEST);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }

}
