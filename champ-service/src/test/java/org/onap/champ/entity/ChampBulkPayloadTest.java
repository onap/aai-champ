/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 Amdocs
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.champ.util.ChampProperties;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.champ.util.ChampProperties")
@PrepareForTest({ChampProperties.class})
public class ChampBulkPayloadTest {

    @Before
    public void setUp() throws Exception {
        mockStatic(ChampProperties.class);
        when(ChampProperties.get(anyString())).thenReturn("");
    }

    @Test
    public void testChampBulkPayload() throws Exception {
        List<ChampBulkOp> ops1 = new ArrayList<ChampBulkOp>();
        List<ChampBulkOp> ops2 = new ArrayList<ChampBulkOp>();
        List<ChampBulkOp> ops3 = new ArrayList<ChampBulkOp>();
        List<ChampBulkOp> ops4 = new ArrayList<ChampBulkOp>();
        
        ChampBulkOp op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.DELETE_OP);
        op.setId("some-id");
        op.setType("pserver");
        op.setProperty("hostname", "some-host");
        ChampObject champObj = op.toChampObject();
        ops1.add(op);
        
        op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.DELETE_OP);
        op.setId("edge-id");
        op.setType("org.onap.aai.BelongsTo");
        op.setLabel("BelongsTo");
        op.setSource("pserver");
        op.setTarget("vserver");
        ChampRelationship champRel = op.toChampRelationship();
        ops2.add(op);
        
        op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.UPDATE_OP);
        op.setId("some-id2");
        op.setType("pserver");
        op.setProperty("hostname", "some-host2");
        ops3.add(op);
        
        op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.ADD_OP);
        op.setId("edge-id2");
        op.setLabel("HostedOn");
        op.setSource("vm");
        op.setTarget("hypervisor");
        ops4.add(op);
        
        ChampBulkPayload payload = new ChampBulkPayload();
        payload.setVertexDeleteOps(ops1);
        payload.setEdgeDeleteOps(ops2);
        payload.setVertexAddModifyOps(ops3);
        payload.setEdgeAddModifyOps(ops4);
        
        String jsonPayload = payload.toJson();
        ChampBulkPayload payload2 = ChampBulkPayload.fromJson(jsonPayload);
        
        assertTrue(payload2.getEdgeAddModifyOps().size() == 1);
        assertTrue(payload2.getEdgeDeleteOps().size() == 1);
        assertTrue(payload2.getVertexAddModifyOps().size() == 1);
        assertTrue(payload2.getVertexDeleteOps().size() == 1);
        
        assertTrue(payload2.getEdgeAddModifyOps().get(0).getId().equals("edge-id2"));
        assertTrue(payload2.getEdgeAddModifyOps().get(0).getLabel().equals("HostedOn"));
        assertTrue(payload2.getEdgeAddModifyOps().get(0).getSource().equals("vm"));
        assertTrue(payload2.getEdgeAddModifyOps().get(0).getTarget().equals("hypervisor"));
        assertTrue(payload2.getEdgeAddModifyOps().get(0).getOperation().equals(ChampBulkPayload.ADD_OP));
        
        assertTrue(payload2.getVertexAddModifyOps().get(0).getId().equals("some-id2"));
        assertTrue(payload2.getVertexAddModifyOps().get(0).getType().equals("pserver"));
        assertTrue(payload2.getVertexAddModifyOps().get(0).getProperties().size() == 1);
        assertTrue(payload2.getVertexAddModifyOps().get(0).getOperation().equals(ChampBulkPayload.UPDATE_OP));
    }

    @Test
    public void testChampBulkResponse() throws Exception {
        
        ChampBulkOp op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.DELETE_OP);
        op.setId("some-id");
        op.setType("pserver");
        op.setProperty("hostname", "some-host");
        ChampObject champObj = op.toChampObject();
        ChampBulkVertexResponse vResp = new ChampBulkVertexResponse("my-vertex", champObj);
        String jsonContent = vResp.toJson();
        ChampBulkVertexResponse vResp2 = ChampBulkVertexResponse.fromJson(jsonContent);
        
        op = new ChampBulkOp();
        op.setOperation(ChampBulkPayload.DELETE_OP);
        op.setId("edge-id");
        op.setType("org.onap.aai.BelongsTo");
        op.setLabel("BelongsTo");
        op.setSource("pserver");
        op.setTarget("vserver");
        ChampRelationship champRel = op.toChampRelationship();
        ChampBulkEdgeResponse eResp = new ChampBulkEdgeResponse("my-edge", champRel);
        jsonContent = eResp.toJson();
        ChampBulkEdgeResponse eResp2 = ChampBulkEdgeResponse.fromJson(jsonContent);

        ChampBulkResponse response = new ChampBulkResponse();
        
        List<ChampBulkVertexResponse> vRespList = new ArrayList<ChampBulkVertexResponse>();
        vRespList.add(vResp2);
        response.setObjects(vRespList);
        
        List<ChampBulkEdgeResponse> vEdgeList = new ArrayList<ChampBulkEdgeResponse>();
        vEdgeList.add(eResp2);
        response.setRelationships(vEdgeList);
        
        jsonContent = response.toJson();
        ChampBulkResponse response2 = ChampBulkResponse.fromJson(jsonContent);
        
        assertTrue(response2.getObjects().size() == 1);
        assertTrue(response2.getRelationships().size() == 1);
        
        assertTrue(response2.getObjects().get(0).getLabel().equals("my-vertex"));
        assertTrue(response2.getObjects().get(0).getVertex() != null);
        
        assertTrue(response2.getRelationships().get(0).getLabel().equals("my-edge"));
        assertTrue(response2.getRelationships().get(0).getEdge() != null);
    }
}
