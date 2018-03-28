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
