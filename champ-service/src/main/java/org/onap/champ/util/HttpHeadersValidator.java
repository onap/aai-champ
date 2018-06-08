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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import org.onap.champ.exception.ChampServiceException;

public class HttpHeadersValidator {

    public void validateRequestHeaders(HttpHeaders headers) throws ChampServiceException {
        String sourceOfTruth = null;
        if (headers.getRequestHeaders().containsKey("X-FromAppId")) {
            sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
        }

        if (sourceOfTruth == null || sourceOfTruth.trim() == "") {
            throw new ChampServiceException("Invalid request, Missing X-FromAppId header", Status.BAD_REQUEST);
        }

        String transId = null;
        if (headers.getRequestHeaders().containsKey("X-TransactionId")) {
            transId = headers.getRequestHeaders().getFirst("X-TransactionId");
        }

        if (transId == null || transId.trim() == "") {
            throw new ChampServiceException("Invalid request, Missing X-TransactionId header", Status.BAD_REQUEST);
        }
    }
}
