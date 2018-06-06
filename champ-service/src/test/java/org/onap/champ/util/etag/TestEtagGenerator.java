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
package org.onap.champ.util.etag;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;

public class TestEtagGenerator {

    private EtagGenerator etagGenerator;

    @Before
    public void init() throws NoSuchAlgorithmException {
        etagGenerator = new EtagGenerator();
    }

    @Test
    public void computeHashForIdenticalChampRelationshipObjects() throws Exception {
        // everything is same
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship11), is(etagGenerator.computeHashForChampRelationship(champRelationship12)));
    }

    @Test
    public void computeHashForIdenticalChampRelationshipObjects1() throws Exception {
        // everything is same
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship11), is(etagGenerator.computeHashForChampRelationship(champRelationship12)));
    }

    @Test
    public void computeHashForIdenticalChampRelationshipObjects2() throws Exception {
        // everything is same
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship11), is(etagGenerator.computeHashForChampRelationship(champRelationship12)));
    }


    @Test
    public void computeHashForChampRelationshipObjectsWithDifferentKey() throws Exception {
        // key is different
        ChampObject sourceChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship21 = new ChampRelationship.Builder(sourceChampObject21, targetChampObject21, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship22 = new ChampRelationship.Builder(sourceChampObject22, targetChampObject22, "tosca.relationships.HostedOn").key("rel1234").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship21), not(etagGenerator.computeHashForChampRelationship(champRelationship22)));
    }

    @Test
    public void computeHashForChampRelationshipObjectsWithDifferentRelationShip() throws Exception {
        // relationship is different
        ChampObject sourceChampObject31 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject31 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship31 = new ChampRelationship.Builder(sourceChampObject31, targetChampObject31, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject32 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject32 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship32 = new ChampRelationship.Builder(sourceChampObject32, targetChampObject32, "tosca.relationships.RelatedTo").key("rel123").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship31), not(etagGenerator.computeHashForChampRelationship(champRelationship32)));
    }

    @Test
    public void computeHashForChampRelationshipObjectsWithDifferentChampObjects() throws Exception {
        // source/target different
        ChampObject sourceChampObject41 = new ChampObject.Builder("pserver").key("a123456").property("prop1", "value1").build();
        ChampObject targetChampObject41 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship41 = new ChampRelationship.Builder(sourceChampObject41, targetChampObject41, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject42 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject42 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship42 = new ChampRelationship.Builder(sourceChampObject42, targetChampObject42, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship41), not(etagGenerator.computeHashForChampRelationship(champRelationship42)));
    }

    @Test
    public void computeHashForChampRelationshipObjectsWithDifferentProperties() throws Exception {
        // property different
        ChampObject sourceChampObject51 = new ChampObject.Builder("pserver").key("a123456").property("prop1", "value1").build();
        ChampObject targetChampObject51 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship51 = new ChampRelationship.Builder(sourceChampObject51, targetChampObject51, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject52 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject52 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship52 = new ChampRelationship.Builder(sourceChampObject52, targetChampObject52, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampRelationship(champRelationship51), not(etagGenerator.computeHashForChampRelationship(champRelationship52)));
    }

    @Test
    public void testComputeHashForIdenticalChampObjects() throws Exception {
        ChampObject champObject1 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject2 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampObject(champObject1), is(etagGenerator.computeHashForChampObject(champObject2)));
    }

    @Test
    public void testComputeHashForEquivalentChampObjects() throws Exception {
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("prop3", "value3");
        properties1.put("prop1", "value1");
        properties1.put("prop2", "value2");
        properties1.put("aai-last-mod-ts", "1234");
        // note aai-last-mod-ts value is different
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("prop3", "value3");
        properties2.put("prop1", "value1");
        properties2.put("prop2", "value2");
        properties2.put("aai-last-mod-ts", "12345");


        ChampObject champObject1 = new ChampObject.Builder("pserver").key("a1234").properties(properties1).build();
        ChampObject champObject2 = new ChampObject.Builder("pserver").key("a1234").properties(properties2).build();
        assertThat(etagGenerator.computeHashForChampObject(champObject1), is(etagGenerator.computeHashForChampObject(champObject2)));
    }

    @Test
    public void testComputeHashForChampObjectsWithDifferentProperties() throws Exception {
        ChampObject champObject3 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject champObject4= new ChampObject.Builder("pserver").key("a12345").property("prop2", "value1").build();
        assertThat(etagGenerator.computeHashForChampObject(champObject3), not(etagGenerator.computeHashForChampObject(champObject4)));
    }

    @Test
    public void testComputeHashForChampObjectsWithDifferentKey() throws Exception {
        ChampObject champObject5 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject champObject6= new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        assertThat(etagGenerator.computeHashForChampObject(champObject5), not(etagGenerator.computeHashForChampObject(champObject6)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampObjects() throws Exception {
        //List 1
        ChampObject champObject11 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        List<ChampObject> champObjects1 = new ArrayList<>();
        champObjects1.add(champObject11);
        champObjects1.add(champObject12);
        // List 2
        ChampObject champObject21 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        List<ChampObject> champObjects2 = new ArrayList<>();
        champObjects2.add(champObject21);
        champObjects2.add(champObject22);

        assertThat(etagGenerator.computeHashForChampObjects(champObjects1), is(etagGenerator.computeHashForChampObjects(champObjects2)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampObjectsWithDifferentOrder() throws Exception {
        //List 1
        ChampObject champObject11 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        List<ChampObject> champObjects1 = new ArrayList<>();
        champObjects1.add(champObject11);
        champObjects1.add(champObject12);
        // List 2
        ChampObject champObject21 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        List<ChampObject> champObjects2 = new ArrayList<>();
        // Different order of the items added.
        champObjects2.add(champObject22);
        champObjects2.add(champObject21);

        assertThat(etagGenerator.computeHashForChampObjects(champObjects1), not(etagGenerator.computeHashForChampObjects(champObjects2)));
    }

    @Test
    public void testComputeHashForDifferentListOfChampObjects() throws Exception {
        //List 1
        ChampObject champObject11 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        List<ChampObject> champObjects1 = new ArrayList<>();
        champObjects1.add(champObject11);
        champObjects1.add(champObject12);
        // List 2
        ChampObject champObject21 = new ChampObject.Builder("pserver").key("a1234").property("prop1", "value1").build();
        ChampObject champObject22 = new ChampObject.Builder("pserver").key("a123456").property("prop2", "value2").build();
        List<ChampObject> champObjects2 = new ArrayList<>();
        champObjects2.add(champObject21);
        champObjects2.add(champObject22);

        assertThat(etagGenerator.computeHashForChampObjects(champObjects1), not(etagGenerator.computeHashForChampObjects(champObjects2)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampRelationships() throws Exception {
        //List 1
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships1 = new ArrayList<>();
        champRelationships1.add(champRelationship11);
        champRelationships1.add(champRelationship12);
        // List 2
        ChampObject sourceChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship21 = new ChampRelationship.Builder(sourceChampObject21, targetChampObject21, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship22 = new ChampRelationship.Builder(sourceChampObject22, targetChampObject22, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships2 = new ArrayList<>();
        champRelationships2.add(champRelationship21);
        champRelationships2.add(champRelationship22);

        assertThat(etagGenerator.computeHashForChampRelationships(champRelationships1), is(etagGenerator.computeHashForChampRelationships(champRelationships2)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampRelationships1() throws Exception {
        //List 1
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").build();
        List<ChampRelationship> champRelationships1 = new ArrayList<>();
        champRelationships1.add(champRelationship11);
        champRelationships1.add(champRelationship12);
        // List 2
        ChampObject sourceChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship21 = new ChampRelationship.Builder(sourceChampObject21, targetChampObject21, "tosca.relationships.HostedOn").key("rel123").build();
        ChampObject sourceChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship22 = new ChampRelationship.Builder(sourceChampObject22, targetChampObject22, "tosca.relationships.HostedOn").key("rel123").build();
        List<ChampRelationship> champRelationships2 = new ArrayList<>();
        champRelationships2.add(champRelationship21);
        champRelationships2.add(champRelationship22);

        assertThat(etagGenerator.computeHashForChampRelationships(champRelationships1), is(etagGenerator.computeHashForChampRelationships(champRelationships2)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampRelationships2() throws Exception {
        //List 1
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships1 = new ArrayList<>();
        champRelationships1.add(champRelationship11);
        champRelationships1.add(champRelationship12);
        // List 2
        ChampObject sourceChampObject21 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject21 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship21 = new ChampRelationship.Builder(sourceChampObject21, targetChampObject21, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject22 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampObject targetChampObject22 = new ChampObject.Builder("pserver").key("a12345").build();
        ChampRelationship champRelationship22 = new ChampRelationship.Builder(sourceChampObject22, targetChampObject22, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships2 = new ArrayList<>();
        champRelationships2.add(champRelationship21);
        champRelationships2.add(champRelationship22);

        assertThat(etagGenerator.computeHashForChampRelationships(champRelationships1), is(etagGenerator.computeHashForChampRelationships(champRelationships2)));
    }

    @Test
    public void testComputeHashForIdenticalListOfChampRelationshipsWithDifferentOrder() throws Exception {

        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a123456").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a123456").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop2", "value2").build();
        List<ChampRelationship> champRelationships1 = new ArrayList<>();
        // List 1
        champRelationships1.add(champRelationship11);
        champRelationships1.add(champRelationship12);
        // List 2, elements added in different order
        List<ChampRelationship> champRelationships2 = new ArrayList<>();
        champRelationships2.add(champRelationship12);
        champRelationships2.add(champRelationship11);

        assertThat(etagGenerator.computeHashForChampRelationships(champRelationships1), not(etagGenerator.computeHashForChampRelationships(champRelationships2)));
    }

    @Test
    public void testComputeHashForDifferntListOfChampRelationships() throws Exception {
      //List 1
        ChampObject sourceChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject11 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship11 = new ChampRelationship.Builder(sourceChampObject11, targetChampObject11, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop1", "value1").build();
        ChampObject targetChampObject12 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship12 = new ChampRelationship.Builder(sourceChampObject12, targetChampObject12, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships1 = new ArrayList<>();
        champRelationships1.add(champRelationship11);
        champRelationships1.add(champRelationship12);
        // List 2
        ChampObject sourceChampObject21 = new ChampObject.Builder("pserver").key("a123456").property("prop1", "value1").build();
        ChampObject targetChampObject21 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship21 = new ChampRelationship.Builder(sourceChampObject21, targetChampObject21, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        ChampObject sourceChampObject22 = new ChampObject.Builder("pserver").key("a123456").property("prop1", "value1").build();
        ChampObject targetChampObject22 = new ChampObject.Builder("pserver").key("a12345").property("prop2", "value2").build();
        ChampRelationship champRelationship22 = new ChampRelationship.Builder(sourceChampObject22, targetChampObject22, "tosca.relationships.HostedOn").key("rel123").property("prop1", "value1").build();
        List<ChampRelationship> champRelationships2 = new ArrayList<>();
        champRelationships2.add(champRelationship21);
        champRelationships2.add(champRelationship22);

        assertThat(etagGenerator.computeHashForChampRelationships(champRelationships1), not(etagGenerator.computeHashForChampRelationships(champRelationships2)));
    }


}

