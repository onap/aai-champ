/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2018 Nokia Intellectual Property. All rights reserved.
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
 */
package org.onap.aai.champcore.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.onap.aai.champcore.model.ChampObjectConstraint.Builder;


public class ChampObjectConstraintTest {

    @Test
    public void verifyEqualsAndHashCodeMethods(){
        ChampObjectConstraint obj1 = new Builder("type").build();
        ChampObjectConstraint obj2 = new Builder("type").build();
        ChampObjectConstraint obj3 = new Builder("type").build();
        ChampObjectConstraint obj4 = new Builder("differentType").build();

        // if
        assertEquals(obj1, obj2);
        assertEquals(obj1.hashCode(), obj2.hashCode());
        //and
        assertEquals(obj1, obj3);
        assertEquals(obj1.hashCode(), obj3.hashCode());
        //then
        assertEquals(obj2, obj3);
        assertEquals(obj2.hashCode(), obj3.hashCode());

        assertNotEquals(obj1, obj4);
        assertNotEquals(obj1.hashCode(), obj4.hashCode());
    }

}