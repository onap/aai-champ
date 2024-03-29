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
package org.onap.champ.entity;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.onap.aai.champcore.model.ChampRelationship;

import java.io.IOException;

public class ChampRelationshipSerializer extends StdSerializer<ChampRelationship> {

    private static final long serialVersionUID = -4057960968983473983L;

    public ChampRelationshipSerializer() {
        this(null);
    }

    protected ChampRelationshipSerializer(Class<ChampRelationship> t) {
        super(t);
    }

    public void serialize( ChampRelationship cr, JsonGenerator jgen, SerializerProvider ser)
            throws IOException, JsonGenerationException {
        jgen.writeStartObject();
        jgen.writeStringField("key", cr.getKeyValue().toString());
        jgen.writeStringField("type", cr.getType());
        jgen.writeObjectField("properties", cr.getProperties());
        jgen.writeObjectField ("source", cr.getSource());
        jgen.writeObjectField ("target", cr.getTarget ());
        jgen.writeEndObject();
    }

}
