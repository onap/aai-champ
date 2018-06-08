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
package org.onap.champ.util;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.champ.exception.ChampServiceException;

public class TestHttpHeadersValidator {

    private static HttpHeadersValidator champRestApi;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        champRestApi = new HttpHeadersValidator();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testValidRequestHeader() throws ChampServiceException {
        champRestApi.validateRequestHeaders(new MockHeaders());
    }

    @Test
    public void testInvalidRequestHeaderXTransactionId() throws ChampServiceException {
        thrown.expect(ChampServiceException.class);
        thrown.expectMessage("Invalid request, Missing X-TransactionId header");

        MockHeaders MockHeaders = new MockHeaders();
        MockHeaders.clearRequestHeader("X-TransactionId");
        champRestApi.validateRequestHeaders(MockHeaders);
    }

    @Test
    public void testInvalidRequestHeaderXFromAppId() throws ChampServiceException {
        thrown.expect(ChampServiceException.class);
        thrown.expectMessage("Invalid request, Missing X-FromAppId header");

        MockHeaders MockHeaders = new MockHeaders();
        MockHeaders.clearRequestHeader("X-FromAppId");
        champRestApi.validateRequestHeaders(MockHeaders);
    }

    @Test
    public void testEmptyRequestHeader() throws ChampServiceException {
        thrown.expect(ChampServiceException.class);
        thrown.expectMessage("Invalid request, Missing X-FromAppId header");

        MockHeaders MockHeaders = new MockHeaders();
        MockHeaders.clearRequestHeader("X-TransactionId", "X-FromAppId");
        champRestApi.validateRequestHeaders(MockHeaders);
    }
}
