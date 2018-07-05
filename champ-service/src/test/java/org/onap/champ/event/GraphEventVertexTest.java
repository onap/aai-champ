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
package org.onap.champ.event;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.champ.event.GraphEvent.GraphEventOperation;

public class GraphEventVertexTest {

    @Test
    public void testToChampObject() throws Exception {

        Map<String, Object> properties = new HashMap<>();
        properties.put("created-ts", new Long("1528470882470"));
        GraphEvent graphEvent = GraphEvent.builder(GraphEventOperation.CREATE)
                .vertex(GraphEventVertex.fromChampObject(new ChampObject.Builder("pserver").properties(properties).build(), "v13")).build();
        GraphEventVertex vertex = graphEvent.getVertex();
        ChampObject champObject = vertex.toChampObject(vertex.toJson());
        assertThat(champObject.getProperty("created-ts").get(), is("1528470882470"));
        assertThat(champObject.getType(), is("pserver"));
    }
}
