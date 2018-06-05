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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.onap.aai.champcore.event.ChampEvent;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.util.TestUtil;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ChampEventEnvelopeTest {

    @Test
    public void testEventEnvelopeFormat() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope.json");

        ChampEvent body = ChampEvent.builder().entity(new ChampObject.Builder("pserver").build()).build();

        String envelope = new ChampEventEnvelope(body).toJson();

        JSONAssert.assertEquals(expectedEnvelope, envelope,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("header.request-id", (o1, o2) -> true),
                        new Customization("header.timestamp", (o1, o2) -> true),
                        new Customization("body.timestamp", (o1, o2) -> true),
                        new Customization("body.transactionId", (o1, o2) -> true)));
    }

    @Test
    public void testRequestIdIsTransactionId() throws Exception {
        ChampEvent body = ChampEvent.builder().entity(new ChampObject.Builder("pserver").build()).build();

        ChampEventEnvelope envelope = new ChampEventEnvelope(body);

        assertThat(envelope.getHeader().getRequestId(), is(envelope.getBody().getTransactionId()));
    }
}
