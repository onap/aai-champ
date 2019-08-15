/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright Â© 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright Â© 2017-2018 Amdocs
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
package org.onap.champ;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.jetty.util.security.Password;
import org.onap.champ.service.logging.LoggingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ImportResource({"file:${SERVICE_BEANS}/*.xml"})
public class ChampApplication extends SpringBootServletInitializer {

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        LoggingUtil.initMdcContext();
        
        String keyStorePassword = System.getProperty("KEY_STORE_PASSWORD");
        if (keyStorePassword == null || keyStorePassword.isEmpty()) {
            throw new RuntimeException("Environment property KEY_STORE_PASSWORD not set");
        }

        Map<String, Object> props = new HashMap<>();
        String deobfuscatedKeyStorePassword = keyStorePassword.startsWith("OBF:") ? Password.deobfuscate(keyStorePassword) : keyStorePassword;
        props.put("server.ssl.key-store-password", deobfuscatedKeyStorePassword);

        String trustStoreLocation = System.getProperty("TRUST_STORE_LOCATION");
        String trustStorePassword = System.getProperty("TRUST_STORE_PASSWORD");
        if (trustStoreLocation != null && trustStorePassword != null) {
            trustStorePassword = trustStorePassword.startsWith("OBF:") ? Password.deobfuscate(trustStorePassword) : trustStorePassword;
            props.put("server.ssl.trust-store", trustStoreLocation);
            props.put("server.ssl.trust-store-password", trustStorePassword);
        }

        String requireClientAuth = System.getenv("REQUIRE_CLIENT_AUTH");
        if (requireClientAuth == null || requireClientAuth.isEmpty()) {
            props.put("server.ssl.client-auth", "need");
        }else {
            props.put("server.ssl.client-auth",requireClientAuth.equals("true")?"need":"want");
        }       

        new ChampApplication().configure(new SpringApplicationBuilder(ChampApplication.class).properties(props))
                .run(args);
    }

    /**
     * Set required trust store system properties using values from application.properties
     */
    @PostConstruct
    public void setSystemProperties() {
        String trustStorePath = env.getProperty("server.ssl.key-store");
        if (trustStorePath != null) {
            String trustStorePassword = env.getProperty("server.ssl.key-store-password");

            if (trustStorePassword != null) {
                System.setProperty("javax.net.ssl.trustStore", trustStorePath);
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            } else {
                throw new IllegalArgumentException("Env property server.ssl.key-store-password not set");
            }
        }
    }
}
