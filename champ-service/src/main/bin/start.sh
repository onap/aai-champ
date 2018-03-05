#!/bin/sh
#
# ============LICENSE_START==========================================
# org.onap.aai
# ===================================================================
# Copyright © 2017 AT&T Intellectual Property. All rights reserved.
# Copyright © 2017 Amdocs
# ===================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END============================================
# ECOMP is a trademark and service mark of AT&T Intellectual Property.
#


BASEDIR="/opt/app/champ-service/"
AJSC_HOME="$BASEDIR"
AJSC_CONF_HOME="$AJSC_HOME/bundleconfig/"

if [ -z "$CONFIG_HOME" ]; then
        echo "CONFIG_HOME must be set in order to start up process"
        exit 1
fi

if [ -z "$KEY_STORE_PASSWORD" ]; then
        echo "KEY_STORE_PASSWORD must be set in order to start up process"
        exit 1
else
        echo "KEY_STORE_PASSWORD=$KEY_STORE_PASSWORD\n" >> $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
fi

if [ -z "$KEY_MANAGER_PASSWORD" ]; then
        echo "KEY_MANAGER_PASSWORD must be set in order to start up process"
        exit 1
else
        echo "KEY_MANAGER_PASSWORD=$KEY_MANAGER_PASSWORD\n" >> $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
fi

# Add any spring bean configuration files to the Gizmo deployment
if [ -n "$SERVICE_BEANS" ]; then
        echo "Adding the following dynamic service beans to the deployment: "
        mkdir -p /tmp/champ-service/v1/conf
        for f in `ls $SERVICE_BEANS`
        do
                cp $SERVICE_BEANS/$f /tmp/champ-service/v1/conf
                echo "Adding dynamic service bean $SERVICE_BEANS/$f"
        done
        jar uf /opt/app/champ-service/services/champ-service_v1.zip* -C /tmp/ champ-service
        rm -rf /tmp/champ-service
fi

CLASSPATH="$AJSC_HOME/lib/*"
CLASSPATH="$CLASSPATH:$AJSC_HOME/extJars/"
CLASSPATH="$CLASSPATH:$AJSC_HOME/etc/"

# Check to see if the provided implementation exists in the image and add it to the classpath
for file in $( find ${BASEDIR}graph-deps/* -maxdepth 0 -type d ); do
        CURRIMPL=$(echo $file | cut -d"/" -f6)
        if [ "x$GRAPHIMPL" = "x$CURRIMPL" ]; then
                CLASSPATH_GRAPHIMPL=$file
                echo "Setting up graph implementation of $GRAPHIMPL"
        else
                SUPPORTED_GRAPHIMPL="$SUPPORTED_GRAPHIMPL $CURRIMPL"
        fi
done
if [ -n "$CLASSPATH_GRAPHIMPL" ]; then
        cp $CLASSPATH_GRAPHIMPL/* $AJSC_HOME/extJars/
else
        echo "Configured graph implementation '$GRAPHIMPL' is not supported. Acceptable implementations are one of: $SUPPORTED_GRAPHIMPL"
        exit 1
fi

PROPS="-DAJSC_HOME=$AJSC_HOME"
PROPS="$PROPS -DAJSC_CONF_HOME=$BASEDIR/bundleconfig/"
PROPS="$PROPS -Dlogback.configurationFile=$BASEDIR/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DAJSC_SHARED_CONFIG=$AJSC_CONF_HOME"
PROPS="$PROPS -DAJSC_SERVICE_NAMESPACE=champ-service"
PROPS="$PROPS -DAJSC_SERVICE_VERSION=v1"
PROPS="$PROPS -Dserver.port=9522"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

echo $CLASSPATH

exec java -Xmx${JVM_MAX_HEAP}m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// sslport=9522
