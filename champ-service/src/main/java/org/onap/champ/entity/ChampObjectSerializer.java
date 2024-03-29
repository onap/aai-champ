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

import java.io.IOException;

import org.onap.aai.champcore.model.ChampObject;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ChampObjectSerializer extends StdSerializer<ChampObject> {
	
	private static final long serialVersionUID = -4057960968983473983L;

	public ChampObjectSerializer() {
	        this(null);
	}
	
	protected ChampObjectSerializer(Class<ChampObject> t) {
		super(t);
	}

	public void serialize(ChampObject co, JsonGenerator jgen, SerializerProvider ser)
			throws IOException, JsonGenerationException {
		jgen.writeStartObject();
        jgen.writeStringField("key", co.getKeyValue().toString());
        jgen.writeStringField("type", co.getType());
        jgen.writeObjectField("properties", co.getProperties());
        jgen.writeEndObject();
	}

}
