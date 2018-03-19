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
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;

public class ChampRelationshipDeserializer extends StdDeserializer<ChampRelationship> {

    private static final long serialVersionUID = -3625275249560680339L;

    public ChampRelationshipDeserializer() {
        this(null);
    }

    protected ChampRelationshipDeserializer(Class<ChampRelationship> t) {
        super(t);
    }

    public ChampRelationship deserialize(JsonParser jparser, DeserializationContext dctx)
            throws IOException, JsonProcessingException {

        JsonNode node = jparser.getCodec().readTree(jparser);
        JsonNode type = node.get("type");
        JsonNode key = node.get("key");
        Map<String, Object> props = new HashMap<>();
        JsonNode propNode = node.get("properties");
        propNode.fields().forEachRemaining((x)->props.put(x.getKey(), x.getValue().asText()));

        JsonNode srcNode = node.get("source");
        JsonNode targetNode = node.get("target");

        ChampObject src = jparser.getCodec ().treeToValue ( srcNode, ChampObject.class );
        ChampObject target = jparser.getCodec ().treeToValue ( targetNode, ChampObject.class );

        ChampRelationship.Builder builder = new ChampRelationship.Builder(src, target, type.asText()).properties(props);

        if(key != null){
            builder.key(key.asText());
        }

        return builder.build();
    }

}
