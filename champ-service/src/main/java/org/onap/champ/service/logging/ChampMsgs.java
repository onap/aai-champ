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
package org.onap.champ.service.logging;

import com.att.eelf.i18n.EELFResourceManager;
import org.onap.aai.cl.eelf.LogMessageEnum;

public enum ChampMsgs implements LogMessageEnum {

  /**
   * Received request {0} {1} from {2}. Sending response: {3}
   *
   * <p>
   * Arguments: {0} = operation {1} = target URL {2} = source {3} = response
   * code
   */
    PROCESS_REST_REQUEST, 
  /**
     * Processed event {0}. Result: {1}.
     *
     * Arguments: {0} = event {1} = result
     */
    PROCESS_EVENT,

    /**
    * Query: {0}
     * Arguments: {0} = query
    */
    QUERY,

    /**
     * Arguments: {0}  = transactionID, {1} = request
     */
    INCOMING_REQUEST,

    /**
     * Arguments: {0}  = HTTP request type, {1} = time to process in milliseconds
     */
    PROCESSED_REQUEST,

    /**
     * Arguments: {0} = transaction ID
     */
    TRANSACTION_NOT_FOUND,

    /**
     * Arguments: {0} = request, {1} = Error
     */
    BAD_REQUEST,

  /**
   * Arguments: {0} = Info
   */
  CHAMP_TX_CACHE,
  
  /**
   * Any info log related to CHAMP_ASYNC_REQUEST_PROCESSOR_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  CHAMP_ASYNC_REQUEST_PROCESSOR_INFO,
  CHAMP_ASYNC_REQUEST_PROCESSOR_WARN,

  /**
   * Any error log related to CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR
   *
   * <p>Arguments:
   * {0} - Error.
   */
  CHAMP_ASYNC_REQUEST_PROCESSOR_ERROR,
  
  /**
   * Any info log related to CHAMP_DATA_SERVICE_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  CHAMP_DATA_SERVICE_INFO,

  /**
   * Any error log related to CHAMP_DATA_SERVICE_INFO
   *
   * <p>Arguments:
   * {0} - Error.
   */
  CHAMP_DATA_SERVICE_ERROR,
  

  /**
   * Any info log related to CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  CHAMP_ASYNC_RESPONSE_PUBLISHER_INFO,

  /**
   * Any error log related to CHAMP_ASYNC_RESPONSE_PUBLISHER_ERROR
   *
   * <p>Arguments:
   * {0} - Error.
   */
  CHAMP_ASYNC_RESPONSE_PUBLISHER_ERROR;
    /**
     * Static initializer to ensure the resource bundles for this class are loaded...
     */
    static {
        EELFResourceManager.loadMessageBundle("logging/ChampMsgs");
    }
}
