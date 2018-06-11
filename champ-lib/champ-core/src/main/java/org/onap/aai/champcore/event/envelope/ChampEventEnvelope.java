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
package org.onap.aai.champcore.event.envelope;

import org.onap.aai.champcore.event.ChampEvent;
import org.onap.aai.champcore.event.envelope.util.GsonUtil;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import com.google.gson.Gson;

public class ChampEventEnvelope {

    private ChampEventHeader header;
    private ChampEvent body;

    /**
     * Serializer/deserializer for converting to/from JSON.
     */
    private static final Gson gson = GsonUtil.createGson();

    public ChampEventEnvelope(ChampEvent event) {
        this.header = new ChampEventHeader.Builder(ChampEventHeader.EventType.UPDATE_NOTIFICATION)
                .requestId(event.getTransactionId()).build();
        this.body = event;
    }

    public ChampEventEnvelope(ChampEventHeader header, ChampEvent body) {
        this.header = header;
        this.body = body;
    }

    public ChampEventHeader getHeader() {
        return header;
    }

    public void setHeader(ChampEventHeader header) {
        this.header = header;
    }

    public ChampEvent getBody() {
        return body;
    }

    public void setBody(ChampEvent body) {
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
     * @throws ChampUnmarshallingException
     */
    public static ChampEventEnvelope fromJson(String json) throws ChampUnmarshallingException {
        try {
            if (json == null || json.isEmpty()) {
                throw new ChampUnmarshallingException("Empty or null JSON string.");
            }
            return gson.fromJson(json, ChampEventEnvelope.class);
        } catch (Exception ex) {
            throw new ChampUnmarshallingException("Unable to parse JSON string: ");
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}