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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChampEventEnvelope {

    private ChampEventHeader header;
    private ChampEvent body;

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
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "Unmarshallable: " + e.getMessage();
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}