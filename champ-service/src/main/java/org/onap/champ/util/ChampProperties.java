/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Modifications Copyright (C) 2019 IBM.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.champ.service.logging.ChampMsgs;

public class ChampProperties {
	
	private static Logger logger = LoggerFactory.getInstance().getLogger(ChampProperties.class.getName());

    private static Properties properties;

    static {
        properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(
            new File(ChampServiceConstants.CHAMP_CONFIG_FILE))
        ) {
            properties.load(fileInputStream);
        } catch (IOException e) {
        	logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "Error while loading properties ", e.getMessage());
            Runtime.getRuntime().halt(1);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void put(String key, String value) {
        properties.setProperty(key, value);
        try (FileOutputStream fileOut = new FileOutputStream(
            new File(ChampServiceConstants.CHAMP_CONFIG_FILE))
        ) {
            properties.store(fileOut, "Added property: " + key);
        } catch (Exception e) {
        	logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "Error while setting properties ", e.getMessage());
        }
    }


}
