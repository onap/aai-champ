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
package org.onap.aai.champcore;

import com.att.eelf.i18n.EELFResourceManager;
import org.onap.aai.cl.eelf.LogMessageEnum;

public enum ChampCoreMsgs implements LogMessageEnum {

  CHAMPCORE_ABSTRACT_LOGGING_CHAMP_GRAPH_INFO,
  CHAMPCORE_ABSTRACT_LOGGING_CHAMP_GRAPH_ERROR,
  CHAMPCORE_ABSTRACT_LOGGING_CHAMP_GRAPH_WARN,
  
  CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_INFO,
  CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_ERROR,
  CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN,
  
  CHAMPCORE_CHAMP_API_IMPL_INFO,
  CHAMPCORE_CHAMP_API_IMPL_ERROR,
  CHAMPCORE_CHAMP_API_IMPL_WARN,
  
  CHAMPCORE_TINKERPOP_TRANSACTION_INFO,
  CHAMPCORE_TINKERPOP_TRANSACTION_ERROR,
  CHAMPCORE_TINKERPOP_TRANSACTION_WARN,
  
  CHAMPCORE_GRAPH_ML_IMPORTER_EXPORTER_INFO,
  CHAMPCORE_GRAPH_ML_IMPORTER_EXPORTER_ERROR,
  CHAMPCORE_GRAPH_ML_IMPORTER_EXPORTER_WARN,

  CHAMPCORE_FORMATTER_INFO,
  CHAMPCORE_FORMATTER_ERROR,
  CHAMPCORE_FORMATTER_WARN;

  
  
  /**
   * Static initializer to ensure the resource bundles for this class are loaded...
   */
  static {
    EELFResourceManager.loadMessageBundle("logging/ChampCoreMsgs");
  }
}
