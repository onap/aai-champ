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
package org.onap.champ.service.logging;

import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.LogLine;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.mdc.MdcContext;

import org.slf4j.MDC;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class LoggingUtil {
  /**
   * Initializes mdc context.
   */
  public static void initMdcContext(HttpServletRequest httpReq, HttpHeaders headers) {
    String fromIp = httpReq.getRemoteAddr();
    String fromAppId = "";
    String transId = null;

    if (headers.getRequestHeaders().getFirst("X-FromAppId") != null) {
      fromAppId = headers.getRequestHeaders().getFirst("X-FromAppId");
    }

    if ((headers.getRequestHeaders().getFirst("X-TransactionId") == null)
        || headers.getRequestHeaders().getFirst("X-TransactionId").isEmpty()) {
      transId = java.util.UUID.randomUUID().toString();
    } else {
      transId = headers.getRequestHeaders().getFirst("X-TransactionId");
    }

    MdcContext.initialize(transId, "ChampService", "", fromAppId, fromIp);
  }

  /**
   * Logs the rest request.
   */
  public static void logRestRequest(Logger logger, Logger auditLogger, HttpServletRequest req, Response response) {
    String respStatusString = "";
    if (Response.Status.fromStatusCode(response.getStatus()) != null) {
      respStatusString = Response.Status.fromStatusCode(response.getStatus()).toString();
    }

    // Generate error log
    logger.info(ChampMsgs.PROCESS_REST_REQUEST, req.getMethod(), req.getRequestURL().toString(),
        req.getRemoteHost(), Integer.toString(response.getStatus()));

    // Generate audit log.
    auditLogger.info(ChampMsgs.PROCESS_REST_REQUEST,
        new LogFields().setField(LogLine.DefinedFields.RESPONSE_CODE, response.getStatus())
            .setField(LogLine.DefinedFields.RESPONSE_DESCRIPTION, respStatusString),
        req.getMethod(), req.getRequestURL().toString(),
        req.getRemoteHost(), Integer.toString(response.getStatus()) + " payload: "
            + (response.getEntity() == null ? "" : response.getEntity().toString()));
    MDC.clear();
  }
  
  public static void logInternalError(Logger logger, Exception ex) {
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    ex.printStackTrace(printWriter);
    logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "Internal error: " + ex.getMessage() + "\n" + writer.toString());
  }
}
