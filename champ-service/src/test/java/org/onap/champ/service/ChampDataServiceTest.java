/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2019 IBM
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
package org.onap.champ.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampElement;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.util.ChampProperties;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.champ.util.ChampProperties")
@PrepareForTest({ChampProperties.class, ChampObject.class, ChampElement.class})
public class ChampDataServiceTest {

    ChampDataService champDataService;
    ChampUUIDService champUUIDService;
    ChampGraph graphImpl;
    ChampTransactionCache cache;

    @Before
    public void setUp() throws Exception {
        mockStatic(ChampProperties.class);
        when(ChampProperties.get(anyString())).thenReturn("");
        champUUIDService = mock(ChampUUIDService.class);
        graphImpl = mock(ChampGraph.class);
        cache = mock(ChampTransactionCache.class);
        champDataService = new ChampDataService(champUUIDService, graphImpl, cache);
    }

    @Test
    public void getObject() throws ChampServiceException, ChampTransactionException, ChampUnmarshallingException {
        ChampTransaction transaction = mock(ChampTransaction.class);
        Optional<ChampObject> retrieved = Optional.of(mock(ChampObject.class));
        ChampObject element = mock(ChampObject.class);

        when(champUUIDService.getObjectbyUUID(anyString(), eq(transaction))).thenReturn(retrieved);
        when(champUUIDService.populateUUIDKey(retrieved.get())).thenReturn(element);
        assertEquals(element, champDataService.getObject("testId", Optional.of(transaction)));
    }
}