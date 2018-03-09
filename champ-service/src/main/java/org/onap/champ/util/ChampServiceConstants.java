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
package org.onap.champ.util;

public class ChampServiceConstants {
  public static final String CHAMP_FILESEP = (System.getProperty("file.separator") == null) ? "/"
      : System.getProperty("file.separator");

  public static final String CHAMP_SPECIFIC_CONFIG = System.getProperty("CONFIG_HOME") + CHAMP_FILESEP;
  public static final String CHAMP_CONFIG_FILE = CHAMP_SPECIFIC_CONFIG + "champ-api.properties";
  public static final String CHAMP_KEY_NAME = "keyName";
  public static final String CHAMP_SOT_NAME = "sourceOfTruthName";
  public static final String CHAMP_CREATED_TS_NAME = "createdTsName";
  public static final String CHAMP_LAST_MOD_TS_NAME = "lastModTsName";
  public static final String CHAMP_COLLECTION_PROPERTIES_KEY = "collectionPropertiesKey";
}