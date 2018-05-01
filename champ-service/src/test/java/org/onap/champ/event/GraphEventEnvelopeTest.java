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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.champ.event.GraphEvent.GraphEventOperation;
import org.onap.champ.event.envelope.GraphEventEnvelope;
import org.onap.champ.util.TestUtil;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class GraphEventEnvelopeTest {

    @Test
    public void testEventEnvelopeFormat() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope.json");

        GraphEvent body = GraphEvent.builder(GraphEventOperation.CREATE)
                .vertex(GraphEventVertex.fromChampObject(new ChampObject.Builder("pserver").build(), "v13")).build();

        String graphEventEnvelope = new GraphEventEnvelope(body).toJson();

        JSONAssert.assertEquals(expectedEnvelope, graphEventEnvelope,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("header.request-id", (o1, o2) -> true),
                        new Customization("header.timestamp", (o1, o2) -> true),
                        new Customization("body.timestamp", (o1, o2) -> true),
                        new Customization("body.transaction-id", (o1, o2) -> true)));
    }

    @Test
    public void testRequestIdIsTransactionId() throws Exception {
        GraphEvent body = GraphEvent.builder(GraphEventOperation.CREATE)
                .vertex(GraphEventVertex.fromChampObject(new ChampObject.Builder("pserver").build(), "v13")).build();

        GraphEventEnvelope envelope = new GraphEventEnvelope(body);

        assertThat(envelope.getHeader().getRequestId(), is(envelope.getBody().getTransactionId()));
    }
}
